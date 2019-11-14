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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KulturtankenService {
    private final RestTemplate restTemplate;
    private final NsrService nsrService;

    @Value("${fint.endpoints.organisation-element}")
    private String organisationElementEndpoint;

    @Value("${fint.endpoints.school}")
    private String schoolEndpoint;

    @Value("${fint.endpoints.basis-group}")
    private String basisGroupEndpoint;

    @Value("${fint.endpoints.level}")
    private String levelEndpoint;

    @Value("${fint.endpoints.teaching-group}")
    private String teachingGroupEndpoint;

    @Value("${fint.endpoints.subject}")
    private String subjectEndpoint;

    private OrganisasjonselementResources organizationElements;
    private SkoleResources schools;
    private ArstrinnResources levels;
    private BasisgruppeResources basisGroups;
    private UndervisningsgruppeResources teachingGroups;
    private FagResources subjects;

    public KulturtankenService(@Qualifier("oauth2RestTemplate") RestTemplate restTemplate, NsrService nsrService) {
        this.restTemplate = restTemplate;
        this.nsrService = nsrService;
    }

    @Cacheable("kulturtanken")
    public Skoleeier getSchoolOwner() {
        fetchData();

        if (organizationElements == null) return new Skoleeier();

        Skoleeier schoolOwner = organizationElements.getContent().stream()
                .filter(o -> o.getSelfLinks().contains(o.getOverordnet().stream().findAny().orElse(null)))
                .map(this::schoolOwner)
                .findFirst()
                .orElse(new Skoleeier());

        schoolOwner.setSkoler(getSchools());

        return schoolOwner;
    }

    private Skoleeier schoolOwner(OrganisasjonselementResource resource) {
        Skoleeier schoolOwner = new Skoleeier();

        Optional<OrganisasjonselementResource> organizationElementResource = Optional.of(resource);
        organizationElementResource.map(OrganisasjonselementResource::getNavn).ifPresent(schoolOwner::setNavn);
        organizationElementResource.map(OrganisasjonselementResource::getOrganisasjonsnummer).map(Identifikator::getIdentifikatorverdi).ifPresent(schoolOwner::setOrganisasjonsnummer);

        schoolOwner.setSkolear(KulturtankenUtil.getSchoolYear(LocalDate.now()));

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
        //schoolResource.map(SkoleResource::getPostadresse).map(this::getVisitingAddress).ifPresent(school::setBesoksadresse);
        schoolResource.map(SkoleResource::getOrganisasjonsnummer).map(Identifikator::getIdentifikatorverdi)
                .map(nsrService::getVisitingAddress).ifPresent(school::setBesoksadresse);
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

        return levelBasisGroupsMap.entrySet().stream()
                .map(this::level)
                .collect(Collectors.toList());
    }

    private Trinn level(Map.Entry<Link, List<BasisgruppeResource>> levelBasisGroupsEntry) {
        Trinn level = new Trinn();

        levels.getContent().stream()
                .filter(l -> l.getSelfLinks().contains(levelBasisGroupsEntry.getKey()))
                .map(ArstrinnResource::getNavn).findAny()
                .ifPresent(level::setNiva);

        List<Basisgruppe> basisGroups = levelBasisGroupsEntry.getValue().stream()
                .map(this::basisGroup)
                .collect(Collectors.toList());

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

        return subjectTeachingGroupsMap.entrySet().stream()
                .map(this::subject)
                .collect(Collectors.toList());
    }

    private Fag subject(Map.Entry<Link, List<UndervisningsgruppeResource>> subjectTeachingGroupsEntry) {
        Fag subject = new Fag();

        subjects.getContent().stream()
                .filter(s -> s.getSelfLinks().contains(subjectTeachingGroupsEntry.getKey()))
                .map(FagResource::getNavn).findAny()
                .ifPresent(subject::setFagkode);

        List<Undervisningsgruppe> teachingGroups = subjectTeachingGroupsEntry.getValue().stream()
                .map(this::teachingGroup)
                .collect(Collectors.toList());

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

    private <T> T get(String uri, Class<T> clazz) {
        return restTemplate.getForObject(uri, clazz);
    }

    @Scheduled(cron = "${fint.kulturtanken.cache-evict-cron:0 0 4 * * *}")
    @CacheEvict(cacheNames = "kulturtanken", allEntries = true)
    public void clearCache() {
        log.info("\uD83D\uDD53 clearing cache...");
    }

    private void fetchData() {
        log.info("Fetching data from FINT API...");
        organizationElements = get(organisationElementEndpoint, OrganisasjonselementResources.class);
        schools = get(schoolEndpoint, SkoleResources.class);
        basisGroups = get(basisGroupEndpoint, BasisgruppeResources.class);
        levels = get(levelEndpoint, ArstrinnResources.class);
        teachingGroups = get(teachingGroupEndpoint, UndervisningsgruppeResources.class);
        subjects = get(subjectEndpoint, FagResources.class);
        log.info("Finished fetching data");
    }
}