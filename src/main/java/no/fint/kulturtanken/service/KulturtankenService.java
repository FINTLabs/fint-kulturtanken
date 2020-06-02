package no.fint.kulturtanken.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.configuration.KulturtankenProperties;
import no.fint.kulturtanken.repository.FintRepository;
import no.fint.kulturtanken.repository.NsrRepository;
import no.fint.kulturtanken.model.*;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.utdanning.elev.BasisgruppeResource;
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResource;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
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
        Skoleeier schoolOwner = nsrRepository.getSchoolOwner(orgId);

        List<Skole> schools = nsrRepository.getSchools(orgId)
                .stream()
                .peek(school -> {
                    if (kulturtankenProperties.getOrganisations().get(orgId).getSource().equals("nsr")) {
                        return;
                    }

                    SkoleResource skoleResource = fintRepository.getSchools(orgId).get(school.getOrganisasjonsnummer());

                    if (skoleResource == null) {
                        return;
                    }

                    List<Trinn> levels = new ArrayList<>();
                    skoleResource.getBasisgruppe()
                            .stream()
                            .map(Link::getHref)
                            .map(String::toLowerCase)
                            .map(fintRepository.getBasisGroups(orgId)::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(this::getLevel))
                            .forEach((key, value) -> Optional.ofNullable(fintRepository.getLevels(orgId).get(key))
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
                    skoleResource.getUndervisningsgruppe()
                            .stream()
                            .map(Link::getHref)
                            .map(String::toLowerCase)
                            .map(fintRepository.getTeachingGroups(orgId)::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(this::getSubject))
                            .forEach((key, value) -> Optional.ofNullable(fintRepository.getSubjects(orgId).get(key))
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
                .orElse("");
    }

    private String getSubject(UndervisningsgruppeResource teachingGroup) {
        return teachingGroup.getFag()
                .stream()
                .map(Link::getHref)
                .map(String::toLowerCase)
                .findAny()
                .orElse("");
    }

    private<T extends FintLinks> String getGrepCode(T resource) {
        return resource.getLinks()
                .getOrDefault("grepreferanse", Collections.emptyList())
                .stream()
                .map(Link::getHref)
                .map(href -> StringUtils.substringAfterLast(href,"/"))
                .findAny()
                .orElse(null);
    }
}