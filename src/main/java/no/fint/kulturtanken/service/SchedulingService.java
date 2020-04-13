package no.fint.kulturtanken.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.configuration.KulturtankenProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SchedulingService {

    private final KulturtankenService kulturtankenService;
    private final KulturtankenProperties kulturtankenProperties;

    public SchedulingService(KulturtankenService kulturtankenService, KulturtankenProperties kulturtankenProperties) {
        this.kulturtankenService = kulturtankenService;
        this.kulturtankenProperties = kulturtankenProperties;
    }

    @Scheduled(cron = "0 0 6 * * MON-FRI")
    @CacheEvict(value = "schoolOwner", allEntries = true, beforeInvocation = true)
    public void updateCache() {
        log.info("Updating cache...");
        kulturtankenProperties.getOrganisations().forEach((key, value) -> {
            kulturtankenService.getSchoolOwner(key);
            log.info("({}) {}", value.getSource(), value.getName());
        });
        log.info("Finished updating cache...");
    }
}
