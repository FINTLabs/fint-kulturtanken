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
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResource;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

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

    public Map<Link, SkoleResource> getSchools(String orgId) {
        KulturtankenProperties.Organisation organisation = kulturtankenProperties.getOrganisations().get(orgId);

        String uri = organisation.getEnvironment().concat("/utdanning/utdanningsprogram/skole");

        OAuth2AuthorizedClient authorizedClient = getAuthorizedClient(organisation);

        return webClient.get()
                .uri(uri)
                .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(SkoleResources.class)
                .flatMapIterable(SkoleResources::getContent)
                .collectMap(this::getSelfLink)
                .block();
    }

    public Map<Link, BasisgruppeResource> getBasisGroups(String orgId) {
        KulturtankenProperties.Organisation organisation = kulturtankenProperties.getOrganisations().get(orgId);

        String uri = organisation.getEnvironment().concat("/utdanning/elev/basisgruppe");

        OAuth2AuthorizedClient authorizedClient = getAuthorizedClient(organisation);

        return webClient.get()
                .uri(uri)
                .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(BasisgruppeResources.class)
                .flatMapIterable(BasisgruppeResources::getContent)
                .collectMap(this::getSelfLink)
                .block();
    }

    public Map<Link, ArstrinnResource> getLevels(String orgId) {
        KulturtankenProperties.Organisation organisation = kulturtankenProperties.getOrganisations().get(orgId);

        String uri = organisation.getEnvironment().concat("/utdanning/utdanningsprogram/arstrinn");

        OAuth2AuthorizedClient authorizedClient = getAuthorizedClient(organisation);

        return webClient.get()
                .uri(uri)
                .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(ArstrinnResources.class)
                .flatMapIterable(ArstrinnResources::getContent)
                .collectMap(this::getSelfLink)
                .block();
    }

    public Map<Link, UndervisningsgruppeResource> getTeachingGroups(String orgId) {
        KulturtankenProperties.Organisation organisation = kulturtankenProperties.getOrganisations().get(orgId);

        String uri = organisation.getEnvironment().concat("/utdanning/timeplan/undervisningsgruppe");

        OAuth2AuthorizedClient authorizedClient = getAuthorizedClient(organisation);

        return webClient.get()
                .uri(uri)
                .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(UndervisningsgruppeResources.class)
                .flatMapIterable(UndervisningsgruppeResources::getContent)
                .collectMap(this::getSelfLink)
                .block();
    }

    public Map<Link, FagResource> getSubjects(String orgId) {
        KulturtankenProperties.Organisation organisation = kulturtankenProperties.getOrganisations().get(orgId);

        String uri = organisation.getEnvironment().concat("/utdanning/timeplan/fag");

        OAuth2AuthorizedClient authorizedClient = getAuthorizedClient(organisation);

        return webClient.get()
                .uri(uri)
                .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(FagResources.class)
                .flatMapIterable(FagResources::getContent)
                .collectMap(this::getSelfLink)
                .block();
    }

    private OAuth2AuthorizedClient getAuthorizedClient(KulturtankenProperties.Organisation organisation) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(organisation.getOrganisationNumber())
                .principal(principal)
                .attributes(attrs -> {
                    attrs.put(OAuth2ParameterNames.USERNAME, organisation.getUsername());
                    attrs.put(OAuth2ParameterNames.PASSWORD, organisation.getPassword());
                }).build();

        return authorizedClientManager.authorize(authorizeRequest);
    }

    private <T extends FintLinks> Link getSelfLink(T resource) {
        return resource.getSelfLinks().stream().findFirst().orElseGet(Link::new);
    }
}
