package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.*;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon;
import no.fint.model.resource.Link;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResources;
import no.fint.model.resource.felles.kompleksedatatyper.AdresseResource;
import no.fint.model.resource.utdanning.elev.BasisgruppeResource;
import no.fint.model.resource.utdanning.elev.BasisgruppeResources;
import no.fint.model.resource.utdanning.timeplan.FagResource;
import no.fint.model.resource.utdanning.timeplan.FagResources;
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResource;
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResources;
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResource;
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResources;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResource;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KulturtankenService {
    private final RestTemplate restTemplate;
    private String bearer;

    private OrganisasjonselementResources organizationElements;
    private SkoleResources schools;
    private ArstrinnResources levels;
    private BasisgruppeResources basisGroups;
    private UndervisningsgruppeResources teachingGroups;
    private FagResources subjects;

    public KulturtankenService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable("kulturtanken")
    public Skoleeier getSchoolOwner(final String bearer) {
        this.bearer = bearer;

        fetchData();

        Skoleeier schoolOwner = (organizationElements != null) ?
                organizationElements.getContent().stream()
                        .filter(o -> o.getSelfLinks().contains(o.getOverordnet().stream().findAny().orElse(null)))
                        .map(this::schoolOwner)
                        .findFirst()
                        .orElse(new Skoleeier()) : new Skoleeier();

        schoolOwner.setSkoler(getSchools());

        return schoolOwner;
    }

    private Skoleeier schoolOwner(OrganisasjonselementResource resource) {
        Skoleeier schoolOwner = new Skoleeier();

        Optional<OrganisasjonselementResource> organizationElementResource = Optional.of(resource);
        organizationElementResource.map(OrganisasjonselementResource::getNavn).ifPresent(schoolOwner::setNavn);
        organizationElementResource.map(OrganisasjonselementResource::getKontaktinformasjon).map(this::getContactInformation).ifPresent(schoolOwner::setKontaktinformasjon);
        organizationElementResource.map(OrganisasjonselementResource::getOrganisasjonsnummer).map(Identifikator::getIdentifikatorverdi).ifPresent(schoolOwner::setOrganisasjonsnummer);

        return schoolOwner;
    }

    private List<Skole> getSchools() {
        if (schools == null) return Collections.emptyList();

        return schools.getContent().stream().map(this::school).collect(Collectors.toList());
    }

    private Skole school(SkoleResource resource) {
        Skole school = new Skole();

        Optional<SkoleResource> schoolResource = Optional.of(resource);
        schoolResource.map(SkoleResource::getNavn).ifPresent(school::setNavn);
        schoolResource.map(SkoleResource::getKontaktinformasjon).map(this::getContactInformation).ifPresent(school::setKontaktinformasjon);
        schoolResource.map(SkoleResource::getPostadresse).map(this::getVisitingAddress).ifPresent(school::setBesoksadresse);
        schoolResource.map(SkoleResource::getOrganisasjonsnummer).map(Identifikator::getIdentifikatorverdi).ifPresent(school::setOrganisasjonsnummer);
        schoolResource.map(SkoleResource::getSkolenummer).map(Identifikator::getIdentifikatorverdi).ifPresent(school::setSkolenummer);
        schoolResource.ifPresent(s -> school.setTrinn(getLevels(s)));
        schoolResource.ifPresent(s -> school.setFag(getSubjects(s)));

        return school;
    }

    private List<Trinn> getLevels(SkoleResource resource) {
        if (basisGroups == null || levels == null) return Collections.emptyList();

        Map<Link, List<BasisgruppeResource>> levelBasisGroupsMap = basisGroups.getContent().stream()
                .filter(b -> resource.getSelfLinks().contains(b.getSkole().stream().findAny().orElse(null)))
                .collect(Collectors.groupingBy(this::getLevelLink));

        return levelBasisGroupsMap.entrySet().stream().map(this::level)
                .filter(l -> l.getNiva() != null).collect(Collectors.toList());
    }

    private Trinn level(Map.Entry<Link, List<BasisgruppeResource>> levelBasisGroupsEntry) {
        Trinn level = new Trinn();

        levels.getContent().stream()
                .filter(l -> l.getSelfLinks().contains(levelBasisGroupsEntry.getKey()))
                .map(ArstrinnResource::getNavn).findAny()
                .ifPresent(level::setNiva);

        List<Basisgruppe> basisGroups = levelBasisGroupsEntry.getValue().stream()
                .filter(b -> b.getElevforhold().size() > 0).map(this::basisGroup).collect(Collectors.toList());

        level.setBasisgrupper(basisGroups);

        return level;
    }

    private Basisgruppe basisGroup(BasisgruppeResource resource) {
        Basisgruppe basisGroup = new Basisgruppe();

        Optional<BasisgruppeResource> basisGroupResource = Optional.of(resource);
        basisGroupResource.map(BasisgruppeResource::getNavn).ifPresent(basisGroup::setNavn);
        basisGroupResource.map(BasisgruppeResource::getElevforhold).map(List::size).ifPresent(basisGroup::setAntall);

        return basisGroup;
    }

    private List<Fag> getSubjects(SkoleResource resource) {
        if (teachingGroups == null || subjects == null) return Collections.emptyList();

        Map<Link, List<UndervisningsgruppeResource>> subjectTeachingGroupsMap = teachingGroups.getContent().stream()
                .filter(t -> resource.getSelfLinks().contains(t.getSkole().stream().findAny().orElse(null)))
                .collect(Collectors.groupingBy(this::getSubjectLink));

        return subjectTeachingGroupsMap.entrySet().stream().map(this::subject)
                .filter(s -> s.getFagkode() != null).collect(Collectors.toList());
    }

    private Fag subject(Map.Entry<Link, List<UndervisningsgruppeResource>> subjectTeachingGroupsEntry) {
        Fag subject = new Fag();

        subjects.getContent().stream()
                .filter(s -> s.getSelfLinks().contains(subjectTeachingGroupsEntry.getKey()))
                .map(FagResource::getNavn).findAny()
                .ifPresent(subject::setFagkode);

        List<Undervisningsgruppe> teachingGroups = subjectTeachingGroupsEntry.getValue().stream()
                .filter(t -> t.getElevforhold().size() > 0).map(this::teachingGroup).collect(Collectors.toList());

        subject.setUndervisningsgrupper(teachingGroups);

        return subject;
    }

    private Undervisningsgruppe teachingGroup(UndervisningsgruppeResource resource) {
        Undervisningsgruppe teachingGroup = new Undervisningsgruppe();

        Optional<UndervisningsgruppeResource> teachingGroupResource = Optional.of(resource);
        teachingGroupResource.map(UndervisningsgruppeResource::getNavn).ifPresent(teachingGroup::setNavn);
        teachingGroupResource.map(UndervisningsgruppeResource::getElevforhold).map(List::size).ifPresent(teachingGroup::setAntall);

        return teachingGroup;
    }

    private no.fint.kulturtanken.model.Kontaktinformasjon getContactInformation(Kontaktinformasjon resource) {
        no.fint.kulturtanken.model.Kontaktinformasjon contactInformation = new no.fint.kulturtanken.model.Kontaktinformasjon();

        Optional<Kontaktinformasjon> contactInfomationResource = Optional.of(resource);
        contactInfomationResource.map(Kontaktinformasjon::getTelefonnummer).ifPresent(contactInformation::setTelefonnummer);
        contactInfomationResource.map(Kontaktinformasjon::getEpostadresse).ifPresent(contactInformation::setEpostadresse);

        return contactInformation;
    }

    private Besoksadresse getVisitingAddress(AdresseResource resource) {
        Besoksadresse visitingAddress = new Besoksadresse();

        Optional<AdresseResource> addressResource = Optional.of(resource);
        addressResource.map(AdresseResource::getAdresselinje).ifPresent(visitingAddress::setAdresselinje);
        addressResource.map(AdresseResource::getPostnummer).ifPresent(visitingAddress::setPostnummer);
        addressResource.map(AdresseResource::getPoststed).ifPresent(visitingAddress::setPoststed);

        return visitingAddress;
    }

    private Link getLevelLink(BasisgruppeResource resource) {
        return resource.getTrinn().stream().findAny().orElse(null);
    }

    private Link getSubjectLink(UndervisningsgruppeResource resource) {
        return resource.getFag().stream().findAny().orElse(null);
    }

    private void fetchData() {
        organizationElements = get("/administrasjon/organisasjon/organisasjonselement", OrganisasjonselementResources.class);
        schools = get("/utdanning/utdanningsprogram/skole", SkoleResources.class);
        basisGroups = get("/utdanning/elev/basisgruppe", BasisgruppeResources.class);
        levels = get("/utdanning/utdanningsprogram/arstrinn", ArstrinnResources.class);
        teachingGroups = get("/utdanning/timeplan/undervisningsgruppe", UndervisningsgruppeResources.class);
        subjects = get("/utdanning/timeplan/fag", FagResources.class);
    }

    @Scheduled(fixedRate = 3600000)
    @CacheEvict(cacheNames = "kulturtanken", allEntries = true)
    public void clearCache() {}

    private <T> T get(String uri, Class<T> clazz) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearer);

        try {
            ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), clazz);
            return response.getBody();
        } catch (RestClientResponseException ex) {
            log.error("Error from RestClient - Status {}, Body {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            return null;
        }
    }
}