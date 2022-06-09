package no.fint.kulturtanken.repository;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.configuration.KulturtankenProperties;
import no.fint.model.resource.AbstractCollectionResources;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.utdanning.elev.*;
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
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class FintRepository {
    private final WebClient webClient;
    private final Authentication principal;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final KulturtankenProperties kulturtankenProperties;

    private final Map<String, Map<String, String>> selfLinks = new HashMap<>();
    private final Map<String, Map<String, FintLinks>> resources = new HashMap<>();

    public FintRepository(WebClient webClient, Authentication principal, OAuth2AuthorizedClientManager authorizedClientManager, KulturtankenProperties kulturtankenProperties) {
        this.webClient = webClient;
        this.principal = principal;
        this.authorizedClientManager = authorizedClientManager;
        this.kulturtankenProperties = kulturtankenProperties;
    }

    public List<SkoleResource> getSchools(String orgId) {
        return getResourcesByType(orgId, SkoleResource.class);
    }

    public BasisgruppeResource getBasisGroupById(String orgId, String groupId) {
        String selfLinks = this.selfLinks.get(orgId).get(groupId);
        return (BasisgruppeResource) resources.get(orgId).get(selfLinks);
    }

    public ArstrinnResource getLevelById(String orgId, String groupId) {
        String selfLinks = this.selfLinks.get(orgId).get(groupId);
        return (ArstrinnResource) resources.get(orgId).get(selfLinks);
    }

    public UndervisningsgruppeResource getTeachingGroupById(String orgId, String groupId) {
        String selfLinks = this.selfLinks.get(orgId).get(groupId);
        return (UndervisningsgruppeResource) resources.get(orgId).get(selfLinks);
    }

    public FagResource getSubjectById(String orgId, String groupId) {
        String selfLinks = this.selfLinks.get(orgId).get(groupId);
        return (FagResource) resources.get(orgId).get(selfLinks);
    }

    private <T> List<T> getResourcesByType(String orgId, Class<T> clazz) {
        return resources.getOrDefault(orgId, Collections.emptyMap())
                .values()
                .stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.toList());
    }

    private <S, T extends AbstractCollectionResources<S>> Flux<S> getResources(String orgId, Class<T> clazz) {
        KulturtankenProperties.Organisation organisation = kulturtankenProperties.getOrganisations().get(orgId);

        return Flux.merge(organisation.getRegistration().values()
                .stream()
                .map(registration -> get(registration, clazz)
                        .flatMapIterable(T::getContent))
                .collect(Collectors.toList()));
    }

    private <T> Mono<T> get(KulturtankenProperties.Registration registration, Class<T> clazz) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(registration.getId())
                .principal(principal)
                .attributes(attrs -> {
                    attrs.put(OAuth2ParameterNames.USERNAME, registration.getUsername());
                    attrs.put(OAuth2ParameterNames.PASSWORD, registration.getPassword());
                }).build();

        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

        String uri = registration.getEnvironment().concat(paths.get(clazz));

        return webClient.get()
                .uri(uri)
                .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(clazz);
    }

    public void updateResources(String orgId) {
        selfLinks.put(orgId, new HashMap<>());
        resources.put(orgId, new HashMap<>());

        Flux.merge(getResources(orgId, SkoleResources.class),
                getResources(orgId, BasisgruppeResources.class),
                getResources(orgId, UndervisningsgruppeResources.class),
                getResources(orgId, ArstrinnResources.class),
                getResources(orgId, FagResources.class))
                .toStream()
                .forEach(resource -> {
                    if (resource.getSelfLinks() != null) {
                        getSelfLinks(resource).forEach(link -> selfLinks.get(orgId).put(link, resource.getSelfLinks().toString()));
                        resources.get(orgId).put(resource.getSelfLinks().toString(), resource);
                    } else {
                        log.debug("Resource with missing selfLinks: {}", resource);
                    }
                });
    }

    private <T extends FintLinks> Stream<String> getSelfLinks(T resource) {
        return resource.getSelfLinks()
                .stream()
                .map(Link::getHref)
                .map(String::toLowerCase);
    }

    private static final Map<Class<?>, String> paths = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>(SkoleResources.class, "/utdanning/utdanningsprogram/skole"),
            new AbstractMap.SimpleImmutableEntry<>(BasisgruppeResources.class, "/utdanning/elev/basisgruppe"),
            new AbstractMap.SimpleImmutableEntry<>(ArstrinnResources.class, "/utdanning/utdanningsprogram/arstrinn"),
            new AbstractMap.SimpleImmutableEntry<>(UndervisningsgruppeResources.class, "/utdanning/timeplan/undervisningsgruppe"),
            new AbstractMap.SimpleImmutableEntry<>(FagResources.class, "/utdanning/timeplan/fag"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}
