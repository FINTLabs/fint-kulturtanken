package no.fint.kulturtanken.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.configuration.KulturtankenProperties;
import no.fint.kulturtanken.repository.FintRepository;
import no.fint.kulturtanken.repository.NsrRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SchedulingService {
    private final KulturtankenProperties kulturtankenProperties;
    private final FintRepository fintRepository;
    private final NsrRepository nsrRepository;

    public SchedulingService(KulturtankenProperties kulturtankenProperties, FintRepository fintRepository, NsrRepository nsrRepository) {
        this.kulturtankenProperties = kulturtankenProperties;
        this.fintRepository = fintRepository;
        this.nsrRepository = nsrRepository;
    }

    @Scheduled(cron = "0 0 6 * * MON-FRI")
    @CacheEvict(value = {"schoolOwner"}, allEntries = true)
    public void updateCache() {
        kulturtankenProperties.getOrganisations().forEach((key, organisation) -> {
            nsrRepository.updateSchoolOwner(key);
            nsrRepository.updateSchools(key);

            if (organisation.getSource().equals("fint")) {
                fintRepository.updateSchools(key);
                fintRepository.updateBasisGroups(key);
                fintRepository.updateLevels(key);
                fintRepository.updateTeachingGroups(key);
                fintRepository.updateSubjects(key);
            }

            log.info(organisation.getName());
        });
    }
}
