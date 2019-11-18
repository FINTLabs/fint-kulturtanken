package no.fint.kulturtanken.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.KulturtankenUtil;
import no.fint.kulturtanken.model.*;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon;
import no.fint.model.resource.Link;
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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KulturtankenService {
    private final NsrService nsrService;
    private final FintService fintService;

    private String orgId;

    public KulturtankenService(NsrService nsrService, FintService fintService) {
        this.nsrService = nsrService;
        this.fintService = fintService;
    }

    public Skoleeier getSchoolOwner(String orgId) {
        this.orgId = orgId;

        Enhet nsrUnit = nsrService.getUnit(orgId);

        Skoleeier schoolOwner = schoolOwner(nsrUnit);

        schoolOwner.setSkoler(getSchools());

        return schoolOwner;
    }

    private Skoleeier schoolOwner(Enhet nsrUnit) {
        Skoleeier schoolOwner = new Skoleeier();

        Optional<Enhet> unit = Optional.of(nsrUnit);
        unit.map(Enhet::getNavn).ifPresent(schoolOwner::setNavn);
        unit.map(Enhet::getOrgNr).ifPresent(schoolOwner::setOrganisasjonsnummer);

        schoolOwner.setSkolear(KulturtankenUtil.getSchoolYear(LocalDate.now()));

        return schoolOwner;
    }

    private List<Skole> getSchools() {
        SkoleResources schools = fintService.getSchools(orgId);

        if (schools == null) return Collections.emptyList();

        return schools.getContent().stream().map(this::school).collect(Collectors.toList());
    }

    private Skole school(SkoleResource resource) {
        Skole school = new Skole();

        Optional<SkoleResource> schoolResource = Optional.of(resource);
        schoolResource.map(SkoleResource::getNavn).ifPresent(school::setNavn);
        schoolResource.map(SkoleResource::getKontaktinformasjon).map(this::getContactInformation).ifPresent(school::setKontaktinformasjon);

        schoolResource.map(SkoleResource::getOrganisasjonsnummer).map(Identifikator::getIdentifikatorverdi).map(nsrService::getUnit)
                .map(this::getVisitingAddress).ifPresent(school::setBesoksadresse);

        schoolResource.map(SkoleResource::getOrganisasjonsnummer).map(Identifikator::getIdentifikatorverdi).ifPresent(school::setOrganisasjonsnummer);
        schoolResource.map(SkoleResource::getSkolenummer).map(Identifikator::getIdentifikatorverdi).ifPresent(school::setSkolenummer);
        schoolResource.ifPresent(s -> school.setTrinn(getLevels(s)));
        schoolResource.ifPresent(s -> school.setFag(getSubjects(s)));

        return school;
    }

    private List<Trinn> getLevels(SkoleResource resource) {
        BasisgruppeResources basisGroups = fintService.getBasisGroups(orgId);

        Map<Link, List<BasisgruppeResource>> levelBasisGroupsMap = basisGroups.getContent().stream()
                .filter(b -> resource.getSelfLinks().contains(b.getSkole().stream().findAny().orElse(null)))
                .collect(Collectors.groupingBy(this::getLevelLink));

        return levelBasisGroupsMap.entrySet().stream()
                .map(this::level)
                .collect(Collectors.toList());
    }

    private Trinn level(Map.Entry<Link, List<BasisgruppeResource>> levelBasisGroupsEntry) {
        ArstrinnResources levels = fintService.getLevels(orgId);

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
        UndervisningsgruppeResources teachingGroups = fintService.getTeachingGroups(orgId);

        Map<Link, List<UndervisningsgruppeResource>> subjectTeachingGroupsMap = teachingGroups.getContent().stream()
                .filter(t -> resource.getSelfLinks().contains(t.getSkole().stream().findAny().orElse(null)))
                .collect(Collectors.groupingBy(this::getSubjectLink));

        return subjectTeachingGroupsMap.entrySet().stream()
                .map(this::subject)
                .collect(Collectors.toList());
    }

    private Fag subject(Map.Entry<Link, List<UndervisningsgruppeResource>> subjectTeachingGroupsEntry) {
        FagResources subjects = fintService.getSubjects(orgId);

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

    private Besoksadresse getVisitingAddress(Enhet nsrUnit) {
        Besoksadresse visitingAddress = new Besoksadresse();

        Optional<Enhet.Adresse> address = Optional.ofNullable(nsrUnit).map(Enhet::getBesoksadresse);
        address.map(Enhet.Adresse::getAdresselinje).map(Collections::singletonList).ifPresent(visitingAddress::setAdresselinje);
        address.map(Enhet.Adresse::getPostnunmmer).ifPresent(visitingAddress::setPostnummer);
        address.map(Enhet.Adresse::getPoststed).ifPresent(visitingAddress::setPoststed);

        return visitingAddress;
    }

    private Link getLevelLink(BasisgruppeResource resource) {
        return resource.getTrinn().stream().findAny().orElse(null);
    }

    private Link getSubjectLink(UndervisningsgruppeResource resource) {
        return resource.getFag().stream().findAny().orElse(null);
    }
}