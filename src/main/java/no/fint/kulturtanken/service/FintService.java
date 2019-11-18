package no.fint.kulturtanken.service;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.utdanning.elev.BasisgruppeResources;
import no.fint.model.resource.utdanning.timeplan.FagResources;
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResources;
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResources;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class FintService {
    private final RestTemplate restTemplate;

    public FintService(@Qualifier("oauth2RestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${fint.endpoints.school}")
    private String schoolEndpoint;

    @Value("${fint.endpoints.basis-group}")
    private String basisGroupEndpoint;

    @Value("${fint.endpoints.level}")
    private String levelEndpoint;

    @Value("${fint.endpoints.teaching-group}")
    private String teachingGroupEndpoint;

    @Value("${fint.endpoints.subject}")
    private String subjectEndpoint;

    private <T> T get(String uri, Class<T> clazz) {
        return restTemplate.getForObject(uri, clazz);
    }

    @Cacheable(value = "schools")
    public SkoleResources getSchools(String orgId) {
        return get(schoolEndpoint, SkoleResources.class);
    }

    @Cacheable(value = "basisGroups")
    public BasisgruppeResources getBasisGroups(String orgId) {
        return get(basisGroupEndpoint, BasisgruppeResources.class);
    }

    @Cacheable(value = "levels")
    public ArstrinnResources getLevels(String orgId) {
        return get(levelEndpoint, ArstrinnResources.class);
    }

    @Cacheable(value = "teachingGroups")
    public UndervisningsgruppeResources getTeachingGroups(String orgId) {
        return get(teachingGroupEndpoint, UndervisningsgruppeResources.class);
    }

    @Cacheable(value = "subjects")
    public FagResources getSubjects(String orgId) {
        return get(subjectEndpoint, FagResources.class);
    }

    @Scheduled(cron = "${fint.kulturtanken.cache-evict-cron:0 0 4 * * MON-FRI}")
    @CacheEvict(cacheNames = {"schools", "basisGroups", "levels", "teachingGroups", "subjects"}, allEntries = true)
    public void clearCache() {
        log.info("\uD83D\uDD53 clearing cache...");
    }
}
