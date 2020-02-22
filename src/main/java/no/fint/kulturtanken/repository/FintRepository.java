package no.fint.kulturtanken.repository;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.configuration.KulturtankenProperties;
import no.fint.model.resource.AbstractCollectionResources;
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
import reactor.core.publisher.Mono;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return getResources(orgId, SkoleResources.class)
                .flatMapIterable(SkoleResources::getContent)
                .collectMap(this::getSelfLink)
                .block();
    }

    public Map<Link, BasisgruppeResource> getBasisGroups(String orgId) {
        return getResources(orgId, BasisgruppeResources.class)
                .flatMapIterable(BasisgruppeResources::getContent)
                .collectMap(this::getSelfLink)
                .block();
    }

    public Map<Link, ArstrinnResource> getLevels(String orgId) {
        return getResources(orgId, ArstrinnResources.class)
                .flatMapIterable(ArstrinnResources::getContent)
                .collectMap(this::getSelfLink)
                .block();
    }

    public Map<Link, UndervisningsgruppeResource> getTeachingGroups(String orgId) {
        return getResources(orgId, UndervisningsgruppeResources.class)
                .flatMapIterable(UndervisningsgruppeResources::getContent)
                .collectMap(this::getSelfLink)
                .block();
    }

    public Map<Link, FagResource> getSubjects(String orgId) {
        return getResources(orgId, FagResources.class)
                .flatMapIterable(FagResources::getContent)
                .collectMap(this::getSelfLink)
                .block();
    }

    public<T extends AbstractCollectionResources<?>> Mono<T> getResources(String orgId, Class<T> clazz) {
        KulturtankenProperties.Organisation organisation = kulturtankenProperties.getOrganisations().get(orgId);

        String uri = organisation.getEnvironment().concat(paths.get(clazz));

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(organisation.getOrganisationNumber())
                .principal(principal)
                .attributes(attrs -> {
                    attrs.put(OAuth2ParameterNames.USERNAME, organisation.getUsername());
                    attrs.put(OAuth2ParameterNames.PASSWORD, organisation.getPassword());
                }).build();

        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

        return webClient.get()
                .uri(uri)
                .attributes(ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(clazz);
    }

    private <T extends FintLinks> Link getSelfLink(T resource) {
        return resource.getSelfLinks().stream().findFirst().orElseGet(Link::new);
    }

    private static final Map<Class<?>, String> paths = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>(SkoleResources.class, "/utdanning/utdanningsprogram/skole"),
            new AbstractMap.SimpleImmutableEntry<>(BasisgruppeResources.class, "/utdanning/elev/basisgruppe"),
            new AbstractMap.SimpleImmutableEntry<>(ArstrinnResources.class, "/utdanning/utdanningsprogram/arstrinn"),
            new AbstractMap.SimpleImmutableEntry<>(UndervisningsgruppeResources.class, "/utdanning/timeplan/undervisningsgruppe"),
            new AbstractMap.SimpleImmutableEntry<>(FagResources.class, "/utdanning/timeplan/fag"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}
