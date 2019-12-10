package no.fint.kulturtanken.repository;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.utdanning.elev.BasisgruppeResource;
import no.fint.model.resource.utdanning.elev.BasisgruppeResources;
import no.fint.model.resource.utdanning.timeplan.FagResource;
import no.fint.model.resource.utdanning.timeplan.FagResources;
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResource;
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResources;
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResource;
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResources;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class FintRepository {

    private final RestTemplate restTemplate;

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

    public FintRepository(@Qualifier("oauth2RestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = "schools", unless = "#result == null")
    public SkoleResources getSchools(String orgId) {
        SkoleResources resources;

        try {
            resources = restTemplate.getForObject(schoolEndpoint, SkoleResources.class);
        } catch (RestClientResponseException ex) {
            log.error(ex.getResponseBodyAsString());
            return null;
        }

        log.info("Updated schools from {}...", schoolEndpoint);

        return resources;
    }

    @Cacheable(value = "basisGroups", unless = "#result == null")
    public Map<Link, Map<Link, List<BasisgruppeResource>>> getBasisGroups(String orgId) {
        BasisgruppeResources resources;

        try {
            resources = restTemplate.getForObject(basisGroupEndpoint, BasisgruppeResources.class);
        } catch (RestClientResponseException ex) {
            log.error(ex.getResponseBodyAsString());
            return null;
        }

        log.info("Updated basis groups from {}...", basisGroupEndpoint);

        return resources != null ? resources.getContent().stream()
                .collect(Collectors.groupingBy(this::getSchoolLink, Collectors.groupingBy(this::getLevelLink))) : null;
    }

    @Cacheable(value = "levels", unless = "#result == null")
    public Map<Link, ArstrinnResource> getLevels(String orgId) {
        ArstrinnResources resources;

        try {
            resources = restTemplate.getForObject(levelEndpoint, ArstrinnResources.class);
        } catch (RestClientResponseException ex) {
            log.error(ex.getResponseBodyAsString());
            return null;
        }

        log.info("Updated levels from {}...", levelEndpoint);

        return resources != null ? resources.getContent().stream()
                .collect(Collectors.toMap(this::getSelfLink, Function.identity(), (a, b) -> a)) : null;
    }

    @Cacheable(value = "teachingGroups", unless = "#result == null")
    public Map<Link, Map<Link, List<UndervisningsgruppeResource>>> getTeachingGroups(String orgId) {
        UndervisningsgruppeResources resources;

        try {
            resources = restTemplate.getForObject(teachingGroupEndpoint, UndervisningsgruppeResources.class);
        } catch (RestClientResponseException ex) {
            log.error(ex.getResponseBodyAsString());
            return null;
        }

        log.info("Updated teaching groups from {}...", teachingGroupEndpoint);

        return resources != null ? resources.getContent().stream()
                .collect(Collectors.groupingBy(this::getSchoolLink, Collectors.groupingBy(this::getSubjectLink))) : null;
    }

    @Cacheable(value = "subjects", unless = "#result == null")
    public Map<Link, FagResource> getSubjects(String orgId) {
        FagResources resources;

        try {
            resources = restTemplate.getForObject(subjectEndpoint, FagResources.class);
        } catch (RestClientResponseException ex) {
            log.error(ex.getResponseBodyAsString());
            return null;
        }

        log.info("Updated subjects from {}...", subjectEndpoint);

        return resources != null ? resources.getContent().stream()
                .collect(Collectors.toMap(this::getSelfLink, Function.identity(), (a, b) -> a)) : null;
    }

    private <T extends FintLinks> Link getSelfLink(T resource) {
        return resource.getSelfLinks().stream().findFirst().orElse(null);
    }

    private <T extends FintLinks> Link getSchoolLink(T resource) {
        return resource.getLinks().get("skole").stream().findFirst().orElse(null);
    }

    private Link getLevelLink(BasisgruppeResource resource) {
        return resource.getTrinn().stream().findFirst().orElse(null);
    }

    private Link getSubjectLink(UndervisningsgruppeResource resource) {
        return resource.getFag().stream().findFirst().orElse(null);
    }

    @Scheduled(cron = "${fint.kulturtanken.cache-evict-cron:0 0 4 * * MON-FRI}")
    @CacheEvict(cacheNames = {"schools", "basisGroups", "levels", "teachingGroups", "subjects"}, allEntries = true)
    public void clearCache() {
        log.info("\uD83D\uDD53 clearing cache...");
    }
}
