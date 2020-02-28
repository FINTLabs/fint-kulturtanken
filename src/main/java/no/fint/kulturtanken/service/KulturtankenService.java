package no.fint.kulturtanken.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.configuration.KulturtankenProperties;
import no.fint.kulturtanken.repository.FintRepository;
import no.fint.kulturtanken.repository.NsrRepository;
import no.fint.kulturtanken.model.*;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.utdanning.elev.BasisgruppeResource;
import no.fint.model.resource.utdanning.timeplan.FagResource;
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResource;
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResource;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResource;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KulturtankenService {

    private final NsrRepository nsrRepository;
    private final FintRepository fintRepository;
    private final KulturtankenProperties kulturtankenProperties;

    public KulturtankenService(NsrRepository nsrRepository, FintRepository fintRepository, KulturtankenProperties kulturtankenProperties) {
        this.nsrRepository = nsrRepository;
        this.fintRepository = fintRepository;
        this.kulturtankenProperties = kulturtankenProperties;
    }

    @Cacheable(value = "schoolOwner")
    public Skoleeier getSchoolOwner(String orgId) {
        Enhet unit = nsrRepository.getUnit(orgId);

        Skoleeier schoolOwner = Skoleeier.fromNsr(unit);

        if (kulturtankenProperties.getOrganisations().get(orgId).getSource().equals("nsr")) {
            List<Skole> schools = unit.getChildRelasjoner().stream()
                    .map(Enhet.ChildRelasjon::getEnhet)
                    .map(Enhet::getOrgNr).distinct()
                    .map(nsrRepository::getUnit)
                    .filter(isValidUnit())
                    .map(Skole::fromNsr)
                    .collect(Collectors.toList());

            schoolOwner.setSkoler(schools);

        } else {
            Map<Link, SkoleResource> schoolMap = fintRepository.getSchools(orgId);
            Map<Link, BasisgruppeResource> basisGroupMap = fintRepository.getBasisGroups(orgId);
            Map<Link, ArstrinnResource> levelMap = fintRepository.getLevels(orgId);
            Map<Link, UndervisningsgruppeResource> teachingGroupMap = fintRepository.getTeachingGroups(orgId);
            Map<Link, FagResource> subjectMap = fintRepository.getSubjects(orgId);

            List<Skole> schools = schoolMap.values().stream()
                    .map(schoolResource -> {
                        Skole school = Skole.fromFint(schoolResource);

                        Optional.ofNullable(school.getOrganisasjonsnummer())
                                .map(nsrRepository::getUnit)
                                .map(Enhet::getBesoksadresse)
                                .map(Besoksadresse::fromNsr)
                                .ifPresent(school::setBesoksadresse);

                        List<Trinn> levels = new ArrayList<>();
                        schoolResource.getBasisgruppe().stream()
                                .map(basisGroupMap::get)
                                .filter(Objects::nonNull)
                                .collect(Collectors.groupingBy(this::getLevel))
                                .forEach((key, value) -> Optional.ofNullable(levelMap.get(key))
                                        .map(this::getGrepCode)
                                        .ifPresent(code -> {
                                            Trinn level = new Trinn();
                                            level.setNiva(code);
                                            level.setBasisgrupper(value.stream()
                                                    .map(Basisgruppe::fromFint)
                                                    .filter(basisGroup -> basisGroup.getAntall() > 0)
                                                    .collect(Collectors.toList()));
                                            levels.add(level);
                                        }));
                        school.setTrinn(levels);

                        List<Fag> subjects = new ArrayList<>();
                        schoolResource.getUndervisningsgruppe().stream()
                                .map(teachingGroupMap::get)
                                .filter(Objects::nonNull)
                                .collect(Collectors.groupingBy(this::getSubject))
                                .forEach((key, value) -> Optional.ofNullable(subjectMap.get(key))
                                        .map(this::getGrepCode)
                                        .ifPresent(code -> {
                                            Fag subject = new Fag();
                                            subject.setFagkode(code);
                                            subject.setUndervisningsgrupper(value.stream()
                                                    .map(Undervisningsgruppe::fromFint)
                                                    .filter(teachingGroup -> teachingGroup.getAntall() > 0)
                                                    .collect(Collectors.toList()));
                                            subjects.add(subject);
                                        }));
                        school.setFag(subjects);

                        return school;
                    })
                    .collect(Collectors.toList());

            schoolOwner.setSkoler(schools);
        }

        return schoolOwner;
    }

    private Link getLevel(BasisgruppeResource basisGroup) {
        return basisGroup.getTrinn().stream().findAny().orElseGet(Link::new);
    }

    private Link getSubject(UndervisningsgruppeResource teachingGroup) {
        return teachingGroup.getFag().stream().findAny().orElseGet(Link::new);
    }

    private<T extends FintLinks> String getGrepCode(T resource) {
        return resource.getLinks().getOrDefault("grepreferanse", Collections.emptyList()).stream()
                .map(Link::getHref)
                .map(href -> StringUtils.substringAfterLast(href,"/"))
                .findAny()
                .orElse(null);
    }

    private Predicate<Enhet> isValidUnit() {
        return unit -> unit.getErOffentligSkole() && unit.getErVideregaaendeSkole() && unit.getErAktiv();
    }
}