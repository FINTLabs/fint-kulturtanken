package no.fint.kulturtanken.repository;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.Enhet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Repository
public class NsrRepository {

    private final RestTemplate restTemplate;

    @Value("${nsr.endpoints.unit}")
    private String unitEndpoint;

    public NsrRepository(@Qualifier("getRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "units", unless = "#result == null")
    public Enhet getUnit(String orgId) {
        Enhet unit;

        try {
            unit = restTemplate.getForObject(unitEndpoint, Enhet.class, orgId);
        } catch (RestClientResponseException ex) {
            log.error(ex.getResponseBodyAsString());
            return null;
        }

        return unit;
    }

    @Scheduled(cron = "${fint.kulturtanken.cache-evict-cron:0 0 4 * * MON-FRI}")
    @CacheEvict(cacheNames = {"units"}, allEntries = true)
    public void clearCache() {
        log.info("\uD83D\uDD53 clearing cache...");
    }
}
