package no.fint.kulturtanken.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.configuration.KulturtankenProperties;
import no.fint.kulturtanken.repository.FintRepository;
import no.fint.kulturtanken.repository.NsrRepository;
import no.fint.kulturtanken.model.*;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.utdanning.elev.BasisgruppeResource;
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResource;
import no.fint.model.utdanning.basisklasser.Gruppe;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
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

    @Cacheable(value = "schoolOwner", unless = "#result == null")
    public Skoleeier getSchoolOwner(String orgId) {
        Skoleeier schoolOwner = nsrRepository.getSchoolOwnerById(orgId);

        if (schoolOwner == null) return null;

        if (kulturtankenProperties.getOrganisations().get(orgId).getSource().equals("nsr")) {
            List<Skole> schools = nsrRepository.getSchools(orgId);
            schoolOwner.setSkoler(schools);
            return schoolOwner;
        }

        List<Skole> schools = fintRepository.getSchools(orgId)
                .stream()
                .map(skoleResource -> {
                    Skole school = nsrRepository.getSchools(orgId)
                            .stream()
                            .filter(s -> Optional.ofNullable(skoleResource.getOrganisasjonsnummer())
                                    .map(Identifikator::getIdentifikatorverdi)
                                    .filter(s.getOrganisasjonsnummer()::equals)
                                    .isPresent())
                            .findAny()
                            .orElse(Skole.fromFint(skoleResource));

                    List<Trinn> levels = new ArrayList<>();

                    Map<String, List<BasisgruppeResource>> basisGroupsByLevel = skoleResource.getBasisgruppe()
                            .stream()
                            .map(Link::getHref)
                            .map(String::toLowerCase)
                            .map(groupId -> fintRepository.getBasisGroupById(orgId, groupId))
                            .filter(Objects::nonNull)
                            .filter(isValidGroup)
                            .collect(Collectors.groupingBy(this::getLevel));

                    basisGroupsByLevel.forEach((key, value) -> Optional.ofNullable(fintRepository.getLevelById(orgId, key))
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

                    Map<String, List<UndervisningsgruppeResource>> teachingGroupsBySubject = skoleResource.getUndervisningsgruppe()
                            .stream()
                            .map(Link::getHref)
                            .map(String::toLowerCase)
                            .map(groupId -> fintRepository.getTeachingGroupById(orgId, groupId))
                            .filter(Objects::nonNull)
                            .filter(isValidGroup)
                            .collect(Collectors.groupingBy(this::getSubject));


                    teachingGroupsBySubject.forEach((key, value) -> Optional.ofNullable(fintRepository.getSubjectById(orgId, key))
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
                }).collect(Collectors.toList());

        schoolOwner.setSkoler(schools);

        return schoolOwner;
    }

    private String getLevel(BasisgruppeResource basisGroup) {
        return basisGroup.getTrinn()
                .stream()
                .map(Link::getHref)
                .map(String::toLowerCase)
                .findAny()
                .orElse(null);
    }

    private String getSubject(UndervisningsgruppeResource teachingGroup) {
        return teachingGroup.getFag()
                .stream()
                .map(Link::getHref)
                .map(String::toLowerCase)
                .findAny()
                .orElse(null);
    }

    private <T extends FintLinks> String getGrepCode(T resource) {
        return resource.getLinks()
                .getOrDefault("grepreferanse", Collections.emptyList())
                .stream()
                .map(Link::getHref)
                .map(href -> StringUtils.substringAfterLast(href, "/"))
                .findAny()
                .orElse(null);
    }

    private final Predicate<? super Gruppe> isValidGroup = group -> {
        List<Periode> period = group.getPeriode();

        if (period.isEmpty()) return true;

        return period.stream()
                .findFirst()
                .filter(begin -> begin.getStart() != null && begin.getStart().compareTo(Date.from(ZonedDateTime.now().toInstant())) <= 0)
                .filter(end -> end.getSlutt() == null || end.getSlutt().compareTo(Date.from(ZonedDateTime.now().toInstant())) >= 0)
                .isPresent();
    };
}