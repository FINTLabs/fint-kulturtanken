package no.fint.kulturtanken.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.config.KulturtankenProperties;
import no.fint.kulturtanken.util.KulturtankenUtil;
import no.fint.kulturtanken.model.*;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon;
import no.fint.model.resource.FintLinks;
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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KulturtankenService {

    private final NsrService nsrService;
    private final FintService fintService;
    private final KulturtankenProperties kulturtankenProperties;

    private String orgId;

    public KulturtankenService(NsrService nsrService, FintService fintService, KulturtankenProperties kulturtankenProperties) {
        this.nsrService = nsrService;
        this.fintService = fintService;
        this.kulturtankenProperties = kulturtankenProperties;
    }

    public Skoleeier getSchoolOwner(final String orgId) {
        this.orgId = orgId;

        Skoleeier schoolOwner = schoolOwner(nsrService.getUnit(orgId));
        schoolOwner.setSkoler(getSchools());

        return schoolOwner;
    }

    private Skoleeier schoolOwner(Enhet nsrUnit) {
        Skoleeier schoolOwner = new Skoleeier();

        schoolOwner.setNavn(nsrUnit.getNavn());
        schoolOwner.setOrganisasjonsnummer(nsrUnit.getOrgNr());
        schoolOwner.setSkolear(KulturtankenUtil.getSchoolYear(LocalDate.now()));

        return schoolOwner;
    }

    private List<Skole> getSchools() {
        if (kulturtankenProperties.getOrganisations().get(orgId).getSource().equals("nsr")) {
            return nsrService.getUnits(orgId).stream()
                    .map(this::school).collect(Collectors.toList());
        } else {
            return fintService.getSchools(orgId).getContent().stream()
                    .map(this::school).collect(Collectors.toList());
        }
    }

    private Skole school(Enhet unit) {
        Skole school = new Skole();

        Optional<Enhet> schoolUnit = Optional.of(unit);
        schoolUnit.map(Enhet::getNavn).ifPresent(school::setNavn);

        no.fint.kulturtanken.model.Kontaktinformasjon contactInformation = new no.fint.kulturtanken.model.Kontaktinformasjon();
        schoolUnit.map(Enhet::getEpost).ifPresent(contactInformation::setEpostadresse);
        schoolUnit.map(Enhet::getTelefon).ifPresent(contactInformation::setTelefonnummer);
        school.setKontaktinformasjon(contactInformation);

        schoolUnit.map(this::getVisitingAddress).ifPresent(school::setBesoksadresse);
        schoolUnit.map(Enhet::getOrgNr).ifPresent(school::setOrganisasjonsnummer);

        school.setTrinn(Collections.emptyList());
        school.setFag(Collections.emptyList());

        return school;
    }

    private Skole school(SkoleResource resource) {
        Skole school = new Skole();

        Optional<SkoleResource> schoolResource = Optional.of(resource);
        schoolResource.map(SkoleResource::getNavn).ifPresent(school::setNavn);
        schoolResource.map(SkoleResource::getKontaktinformasjon).map(this::getContactInformation).ifPresent(school::setKontaktinformasjon);

        schoolResource.map(SkoleResource::getOrganisasjonsnummer).map(Identifikator::getIdentifikatorverdi).map(nsrService::getUnit)
                .map(this::getVisitingAddress).ifPresent(school::setBesoksadresse);

        schoolResource.map(SkoleResource::getOrganisasjonsnummer).map(Identifikator::getIdentifikatorverdi).ifPresent(school::setOrganisasjonsnummer);

        schoolResource.ifPresent(s -> school.setTrinn(getLevels(s)));
        schoolResource.ifPresent(s -> school.setFag(getSubjects(s)));

        return school;
    }

    private List<Trinn> getLevels(SkoleResource resource) {
        Map<Link, List<BasisgruppeResource>> basisGroupsByLevel = fintService.getBasisGroups(orgId).getContent().stream()
                .filter(hasSchoolRelation(resource))
                .collect(Collectors.groupingBy(this::getLevelLink));

        return basisGroupsByLevel.entrySet().stream()
                .map(this::level)
                .collect(Collectors.toList());
    }

    private Trinn level(Map.Entry<Link, List<BasisgruppeResource>> basisGroupByLevel) {
        Trinn level = new Trinn();

        fintService.getLevels(orgId).getContent().stream()
                .filter(l -> l.getSelfLinks().contains(basisGroupByLevel.getKey()))
                .map(ArstrinnResource::getNavn).findAny()
                .ifPresent(level::setNiva);

        List<Basisgruppe> basisGroups = basisGroupByLevel.getValue().stream()
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
        Map<Link, List<UndervisningsgruppeResource>> teachingGroupsBySubject = fintService.getTeachingGroups(orgId).getContent().stream()
                .filter(hasSchoolRelation(resource))
                .collect(Collectors.groupingBy(this::getSubjectLink));

        return teachingGroupsBySubject.entrySet().stream()
                .map(this::subject)
                .collect(Collectors.toList());
    }

    private Fag subject(Map.Entry<Link, List<UndervisningsgruppeResource>> subjectTeachingGroupsEntry) {
        Fag subject = new Fag();

        fintService.getSubjects(orgId).getContent().stream()
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
        address.map(Enhet.Adresse::getAdress).map(Collections::singletonList).ifPresent(visitingAddress::setAdresselinje);
        address.map(Enhet.Adresse::getPostnr).ifPresent(visitingAddress::setPostnummer);
        address.map(Enhet.Adresse::getPoststed).ifPresent(visitingAddress::setPoststed);

        return visitingAddress;
    }

    private Link getLevelLink(BasisgruppeResource resource) {
        return resource.getTrinn().stream().findFirst().orElse(null);
    }

    private Link getSubjectLink(UndervisningsgruppeResource resource) {
        return resource.getFag().stream().findFirst().orElse(null);
    }

    private<T extends FintLinks> Predicate<T> hasSchoolRelation(T school) {
        return resource -> resource.getLinks().get("skole").contains(school.getSelfLinks().stream().findFirst().orElse(null));
    }
}