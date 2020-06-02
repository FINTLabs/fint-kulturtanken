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
            nsrRepository.getSchoolOwner(key);
            nsrRepository.getSchools(key);

            if (organisation.getSource().equals("fint")) {
                fintRepository.getSchools(key);
                fintRepository.getBasisGroups(key);
                fintRepository.getLevels(key);
                fintRepository.getTeachingGroups(key);
                fintRepository.getSubjects(key);
            }

            log.info(organisation.getName());
        });
    }
}
