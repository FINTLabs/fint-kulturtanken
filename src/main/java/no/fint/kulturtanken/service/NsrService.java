package no.fint.kulturtanken.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.Enhet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NsrService {

    private final RestTemplate restTemplate;

    @Value("${nsr.endpoints.unit}")
    private String unitEndpoint;

    public NsrService(@Qualifier("getRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "units")
    public Enhet getUnit(String orgId) {
        return restTemplate.getForObject(unitEndpoint, Enhet.class, orgId);
    }

    @Cacheable(value = "childUnits")
    public List<Enhet> getUnits(String orgId) {
        return getUnit(orgId).getChildRelasjoner().stream()
                .map(Enhet.ChildRelasjon::getEnhet)
                .map(Enhet::getOrgNr).distinct()
                .map(unitId -> restTemplate.getForObject(unitEndpoint, Enhet.class, unitId))
                .filter(isValidUnit())
                .collect(Collectors.toList());
    }

    private Predicate<Enhet> isValidUnit() {
        return unit -> unit.getErOffentligSkole() && unit.getErVideregaaendeSkole() && unit.getErAktiv();
    }

    @Scheduled(cron = "${fint.kulturtanken.cache-evict-cron:0 0 4 * * MON-FRI}")
    @CacheEvict(cacheNames = {"units", "childUnits"}, allEntries = true)
    public void clearCache() {
        log.info("\uD83D\uDD53 clearing cache...");
    }
}
