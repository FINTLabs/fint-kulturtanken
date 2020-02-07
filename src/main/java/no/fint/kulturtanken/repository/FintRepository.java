package no.fint.kulturtanken.repository;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.configuration.KulturtankenProperties;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FintRepository {

    private final WebClient webClient;
    private final Authentication principal;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final KulturtankenProperties kulturtankenProperties;

    public FintRepository(WebClient webClient, Authentication principal, OAuth2AuthorizedClientManager authorizedClientManager, KulturtankenProperties kulturtankenProperties) {
        this.webClient = webClient;
        this.principal = principal;
        this.authorizedClientManager = authorizedClientManager;
        this.kulturtankenProperties = kulturtankenProperties;
    }

    @Cacheable(value = "schools", unless = "#result == null")
    public SkoleResources getSchools(String orgId) {
        SkoleResources resources;

        String uri = kulturtankenProperties.getOrganisations().get(orgId).getEnvironment().concat("/utdanning/utdanningsprogram/skole");

        try {
            resources = webClient.get()
                    .uri(uri)
                    .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(getAuthorizedClient(orgId)))
                    .retrieve()
                    .bodyToMono(SkoleResources.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error(ex.getResponseBodyAsString());
            return null;
        }

        log.info("({}) Updated schools from {}...", orgId, uri);

        return resources;
    }

    @Cacheable(value = "basisGroups", unless = "#result == null")
    public Map<Link, BasisgruppeResource> getBasisGroups(String orgId) {
        BasisgruppeResources resources;

        String uri = kulturtankenProperties.getOrganisations().get(orgId).getEnvironment().concat("/utdanning/elev/basisgruppe");

        try {
            resources = webClient.get()
                    .uri(uri)
                    .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(getAuthorizedClient(orgId)))
                    .retrieve()
                    .bodyToMono(BasisgruppeResources.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error(ex.getResponseBodyAsString());
            return null;
        }

        log.info("({}) Updated basis groups from {}...", orgId, uri);

        return resources != null ? resources.getContent().stream()
                .collect(Collectors.toMap(this::getSelfLink, Function.identity(), (a, b) -> a)) : null;
    }

    @Cacheable(value = "levels", unless = "#result == null")
    public Map<Link, ArstrinnResource> getLevels(String orgId) {
        ArstrinnResources resources;

        String uri = kulturtankenProperties.getOrganisations().get(orgId).getEnvironment().concat("/utdanning/utdanningsprogram/arstrinn");

        try {
            resources = webClient.get()
                    .uri(uri)
                    .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(getAuthorizedClient(orgId)))
                    .retrieve()
                    .bodyToMono(ArstrinnResources.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error(ex.getResponseBodyAsString());
            return null;
        }

        log.info("({}) Updated levels from {}...", orgId, uri);

        return resources != null ? resources.getContent().stream()
                .collect(Collectors.toMap(this::getSelfLink, Function.identity(), (a, b) -> a)) : null;
    }

    @Cacheable(value = "teachingGroups", unless = "#result == null")
    public Map<Link, UndervisningsgruppeResource> getTeachingGroups(String orgId) {
        UndervisningsgruppeResources resources;

        String uri = kulturtankenProperties.getOrganisations().get(orgId).getEnvironment().concat("/utdanning/timeplan/undervisningsgruppe");

        try {
            resources = webClient.get()
                    .uri(uri)
                    .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(getAuthorizedClient(orgId)))
                    .retrieve()
                    .bodyToMono(UndervisningsgruppeResources.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error(ex.getResponseBodyAsString());
            return null;
        }

        log.info("({}) Updated teaching groups from {}...", orgId, uri);

        return resources != null ? resources.getContent().stream()
                .collect(Collectors.toMap(this::getSelfLink, Function.identity(), (a, b) -> a)) : null;
    }

    @Cacheable(value = "subjects", unless = "#result == null")
    public Map<Link, FagResource> getSubjects(String orgId) {
        FagResources resources;

        String uri = kulturtankenProperties.getOrganisations().get(orgId).getEnvironment().concat("/utdanning/timeplan/fag");

        try {
            resources = webClient.get()
                    .uri(uri)
                    .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(getAuthorizedClient(orgId)))
                    .retrieve()
                    .bodyToMono(FagResources.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error(ex.getResponseBodyAsString());
            return null;
        }

        log.info("({}) Updated subjects from {}...", orgId, uri);

        return resources != null ? resources.getContent().stream()
                .collect(Collectors.toMap(this::getSelfLink, Function.identity(), (a, b) -> a)) : null;
    }

    private OAuth2AuthorizedClient getAuthorizedClient(String orgId) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(orgId)
                .principal(principal)
                .attributes(attrs -> {
                    attrs.put(OAuth2ParameterNames.USERNAME, kulturtankenProperties.getOrganisations().get(orgId).getUsername());
                    attrs.put(OAuth2ParameterNames.PASSWORD, kulturtankenProperties.getOrganisations().get(orgId).getPassword());
                }).build();

        return authorizedClientManager.authorize(authorizeRequest);
    }

    private <T extends FintLinks> Link getSelfLink(T resource) {
        return resource.getSelfLinks().stream().findFirst().orElseGet(Link::new);
    }

    @Scheduled(cron = "${fint.kulturtanken.cache-evict-cron:0 0 4 * * MON-FRI}")
    @CacheEvict(cacheNames = {"schools", "basisGroups", "levels", "teachingGroups", "subjects"}, allEntries = true)
    public void clearCache() {
        log.info("\uD83D\uDD53 clearing cache...");
    }
}
