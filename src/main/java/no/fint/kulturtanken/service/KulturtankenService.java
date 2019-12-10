package no.fint.kulturtanken.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.configuration.KulturtankenProperties;
import no.fint.kulturtanken.repository.FintRepository;
import no.fint.kulturtanken.repository.NsrRepository;
import no.fint.kulturtanken.model.*;
import no.fint.model.resource.Link;
import no.fint.model.resource.utdanning.elev.BasisgruppeResource;
import no.fint.model.resource.utdanning.timeplan.FagResource;
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResource;
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResource;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResource;
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
            List<Skole> schools = fintRepository.getSchools(orgId).getContent().stream()
                    .map(s -> {
                        Skole school = Skole.fromFint(s);
                        Optional.ofNullable(nsrRepository.getUnit(school.getOrganisasjonsnummer()))
                                .map(Enhet::getBesoksadresse).map(Besoksadresse::fromNsr).ifPresent(school::setBesoksadresse);
                        school.setTrinn(getLevels(s, orgId));
                        school.setFag(getSubjects(s, orgId));
                        return school;
                    })
                    .collect(Collectors.toList());

            schoolOwner.setSkoler(schools);
        }

        return schoolOwner;
    }

    private List<Trinn> getLevels(SkoleResource school, String orgId) {
        Map<Link, Map<Link, List<BasisgruppeResource>>> basisGroupsByLevelAndSchool = fintRepository.getBasisGroups(orgId);
        Map<Link, ArstrinnResource> levelBySelf = fintRepository.getLevels(orgId);

        if (basisGroupsByLevelAndSchool == null || levelBySelf == null) return Collections.emptyList();

        List<Trinn> levels = new ArrayList<>();

        school.getSelfLinks().stream().findFirst().ifPresent(link -> {
            if (basisGroupsByLevelAndSchool.containsKey(link)) {
                Map<Link, List<BasisgruppeResource>> basisGroupsByLevel = basisGroupsByLevelAndSchool.get(link);
                levelBySelf.forEach((key, value) -> {
                    if (basisGroupsByLevel.containsKey(key)) {
                        Trinn level = new Trinn();
                        level.setNiva(value.getNavn());
                        level.setBasisgrupper(basisGroupsByLevel.get(key).stream()
                                .map(Basisgruppe::fromFint)
                                .collect(Collectors.toList()));
                        levels.add(level);
                    }
                });
            }
        });

        return levels;
    }

    private List<Fag> getSubjects(SkoleResource school, String orgId) {
        Map<Link, Map<Link, List<UndervisningsgruppeResource>>> teachingGroupsByLevelAndSchool = fintRepository.getTeachingGroups(orgId);
        Map<Link, FagResource> subjectsBySelf = fintRepository.getSubjects(orgId);

        if (teachingGroupsByLevelAndSchool == null || subjectsBySelf == null) return Collections.emptyList();

        List<Fag> subjects = new ArrayList<>();

        school.getSelfLinks().stream().findFirst().ifPresent(link -> {
            if (teachingGroupsByLevelAndSchool.containsKey(link)) {
                Map<Link, List<UndervisningsgruppeResource>> teachingGroupsByLevel = teachingGroupsByLevelAndSchool.get(link);
                subjectsBySelf.forEach((key, value) -> {
                    if (teachingGroupsByLevel.containsKey(key)) {
                        Fag subject = new Fag();
                        subject.setFagkode(value.getNavn());
                        subject.setUndervisningsgrupper(teachingGroupsByLevel.get(key).stream()
                                .map(Undervisningsgruppe::fromFint)
                                .collect(Collectors.toList()));
                        subjects.add(subject);
                    }
                });
            }
        });

        return subjects;
    }

    private Predicate<Enhet> isValidUnit() {
        return unit -> unit.getErOffentligSkole() && unit.getErVideregaaendeSkole() && unit.getErAktiv();
    }
}