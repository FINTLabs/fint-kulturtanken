package no.fint.kulturtanken.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.KulturtankenProperties;
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

    @Scheduled(initialDelay = 5000, fixedDelay = 86400000)
    @CacheEvict(value = {"schoolOwner"}, allEntries = true)
    public void updateCache() {
        kulturtankenProperties.getOrganisations().forEach((orgId, organisation) -> {
            nsrRepository.updateResources(orgId);

            if (organisation.getSource().equals("fint")) {
                fintRepository.updateResources(orgId);
            }

            log.info(organisation.getName());
        });
    }
}
