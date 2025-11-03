package no.fint.kulturtanken.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.configuration.KulturtankenProperties;
import no.fint.kulturtanken.model.*;
import no.fint.kulturtanken.repository.FintRepository;
import no.fint.kulturtanken.repository.NsrRepository;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.utdanning.elev.KlasseResource;
import no.fint.model.resource.utdanning.kodeverk.SkolearResource;
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling Kulturtanken-related operations, such as retrieving school owners,
 * schools, levels, subjects, and validating groups.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KulturtankenService {

    private final NsrRepository nsrRepository;
    private final FintRepository fintRepository;
    private final KulturtankenProperties kulturtankenProperties;
    private final Clock clock;

    /**
     * Retrieves the school owner for the given organization ID.
     * Populates the school owner with schools, levels, and subjects.
     *
     * @param orgId the organization ID
     * @return the {@link Skoleeier} for the given orgId, or null if not found
     */
    @Cacheable(value = "schoolOwner", unless = "#result == null")
    public Skoleeier getSchoolOwner(String orgId) {
        // Fetch school owner from NSR
        Skoleeier schoolOwner = nsrRepository.getSchoolOwnerById(orgId);

        if (schoolOwner == null) return null;


        // If School already exist in kulturtankenProperties, fetch schools from NSR
        // and set school on schoolOwner and return immediately
        if (kulturtankenProperties.getOrganisations().get(orgId).getSource().equals("nsr")) {
            List<Skole> schools = nsrRepository.getSchools(orgId);
            schoolOwner.setSkoler(schools);
            return schoolOwner;
        }

        // Fetch schools from FINT and populate with levels and subjects
        List<Skole> schools = fintRepository.getSchools(orgId)
                .stream()
                // for every school in FINT
                .map(skoleResource -> {
                    // Get school from NSR and keep it if it exists, otherwise create new
                    Skole school = nsrRepository.getSchools(orgId)
                            .stream()
                            .filter(s -> Optional.ofNullable(skoleResource.getOrganisasjonsnummer())
                                    .map(Identifikator::getIdentifikatorverdi)
                                    .filter(s.getOrganisasjonsnummer()::equals)
                                    .isPresent())
                            .findAny()
                            .orElse(Skole.fromFint(skoleResource));

                    List<Trinn> levels = new ArrayList<>();

                    // Get classes and group them by level
                    Map<String, List<KlasseResource>> schoolClassesByLevel = skoleResource.getKlasse()
                            .stream()
                            .map(Link::getHref)
                            .map(String::toLowerCase)
                            .map(groupId -> fintRepository.getKlasseById(orgId, groupId))
                            .filter(Objects::nonNull)
                            .filter(group -> isValidGroup(group, orgId))
                            .collect(Collectors.groupingBy(this::getLevel));

                    // For each level, create Trinn and add klasser
                    schoolClassesByLevel.forEach((key, value) -> Optional.ofNullable(fintRepository.getLevelById(orgId, key))
                            .map(this::getGrepCode)
                            .ifPresent(code -> {
                                Trinn level = new Trinn();
                                level.setNiva(code);
                                level.setKlasser(value.stream()
                                        .map(Klasse::fromFint)
                                        .filter(klasse -> klasse.getAntall() > 0)
                                        .collect(Collectors.toList()));
                                levels.add(level);
                            }));

                    school.setTrinn(levels);

                    List<Fag> subjects = new ArrayList<>();

                    // Build teachingGroups and group them by subjects
                    Map<String, List<UndervisningsgruppeResource>> teachingGroupsBySubject = skoleResource.getUndervisningsgruppe()
                            .stream()
                            .map(Link::getHref)
                            .map(String::toLowerCase)
                            .map(groupId -> fintRepository.getTeachingGroupById(orgId, groupId))
                            .filter(Objects::nonNull)
                            .filter(group -> isValidGroup(group, orgId))
                            .collect(Collectors.groupingBy(this::getSubject));


                    // Populate subjects with courses
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

    /**
     * Gets the level identifier for a given {@link KlasseResource}.
     *
     * @param klasse the class resource
     * @return the level identifier, or "none" if not found
     */
    private String getLevel(KlasseResource klasse) {
        return klasse.getTrinn()
                .stream()
                .map(Link::getHref)
                .map(String::toLowerCase)
                .findAny()
                .orElse("none");
    }

    /**
     * Gets the subject identifier for a given {@link UndervisningsgruppeResource}.
     *
     * @param teachingGroup the teaching group resource
     * @return the subject identifier, or "none" if not found
     */
    private String getSubject(UndervisningsgruppeResource teachingGroup) {
        return teachingGroup.getFag()
                .stream()
                .map(Link::getHref)
                .map(String::toLowerCase)
                .findAny()
                .orElse("none");
    }

    /**
     * Gets the GREP code from a Fint resource.
     *
     * @param resource the Fint resource
     * @param <T>      type extending {@link FintLinks}
     * @return the GREP code, or null if not found
     */
    private <T extends FintLinks> String getGrepCode(T resource) {
        return resource.getLinks()
                .getOrDefault("grepreferanse", Collections.emptyList())
                .stream()
                .map(Link::getHref)
                .map(href -> StringUtils.substringAfterLast(href, "/"))
                .findAny()
                .orElse(null);
    }

    /**
     * Validates if a group (class or teaching group) is valid based on its associated school year.
     *
     * @param group the group resource (either {@link KlasseResource} or {@link UndervisningsgruppeResource})
     * @param orgId the organization ID
     * @param <T>   type of the group resource
     * @return True if the group is valid, else false
     */
    private <T> boolean isValidGroup(T group, String orgId) {
        String systemId = null;
        if (group instanceof KlasseResource) {
            systemId = ((KlasseResource) group).getSystemId().toString();
        } else if (group instanceof UndervisningsgruppeResource) {
            systemId = ((UndervisningsgruppeResource) group).getSystemId().toString();
        } else {
            return false;
        }

        SkolearResource skolear = fintRepository.getSkolearById(systemId, orgId);
        if (skolear == null) return false;
        Periode period = skolear.getGyldighetsperiode();

        return isWithinPeriod(period);
    }

    /**
     * Checks if the {@link Periode} start and slutt is within today's date.
     * @param periode the period to check
     * @return True if within period, else false
     */
    private boolean isWithinPeriod(Periode periode) {
        if (periode == null) return false;
        Instant now = Date.from(Instant.now(clock)).toInstant();
        Date start = periode.getStart();
        Date end = periode.getSlutt();

        if (start == null || end == null) return false;
        return now.isAfter(start.toInstant()) && now.isBefore(end.toInstant());
    }





}