package no.fint.kulturtanken.repository;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.configuration.KulturtankenProperties;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
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

    private final Map<String, Map<String, SkoleResource>> schools = new HashMap<>();
    private final Map<String, Map<String, BasisgruppeResource>> basisGroups = new HashMap<>();
    private final Map<String, Map<String, ArstrinnResource>> levels = new HashMap<>();
    private final Map<String, Map<String, UndervisningsgruppeResource>> teachingGroups = new HashMap<>();
    private final Map<String, Map<String, FagResource>> subjects = new HashMap<>();

    public FintRepository(WebClient webClient, Authentication principal, OAuth2AuthorizedClientManager authorizedClientManager, KulturtankenProperties kulturtankenProperties) {
        this.webClient = webClient;
        this.principal = principal;
        this.authorizedClientManager = authorizedClientManager;
        this.kulturtankenProperties = kulturtankenProperties;
    }

    public Map<String, SkoleResource> getSchools(String orgId) {
        Map<String, SkoleResource> resources = schools.get(orgId);

        if (resources == null || resources.size() == 0) {
            updateSchools(orgId);
        }

        return schools.get(orgId);
    }

    public void updateSchools(String orgId) {
        schools.clear();

        Map<String, SkoleResource> resources = getResources(orgId, SkoleResources.class)
                .filter(skoleResource -> Optional.ofNullable(skoleResource.getOrganisasjonsnummer())
                        .map(Identifikator::getIdentifikatorverdi)
                        .isPresent())
                .collectMap(skoleResource -> skoleResource.getOrganisasjonsnummer().getIdentifikatorverdi())
                .block();

        schools.put(orgId, resources);
    }

    public Map<String, BasisgruppeResource> getBasisGroups(String orgId) {
        Map<String, BasisgruppeResource> resources = basisGroups.get(orgId);

        if (resources == null || resources.size() == 0) {
            updateBasisGroups(orgId);
        }

        return basisGroups.get(orgId);
    }

    public void updateBasisGroups(String orgId) {
        basisGroups.clear();

        Map<String, BasisgruppeResource> resources = getResources(orgId, BasisgruppeResources.class)
                .collectMap(this::getSelfLink)
                .block();

        basisGroups.put(orgId, resources);
    }


    public Map<String, ArstrinnResource> getLevels(String orgId) {
        Map<String, ArstrinnResource> resources = levels.get(orgId);

        if (resources == null || resources.size() == 0) {
            updateLevels(orgId);
        }

        return levels.get(orgId);
    }

    public void updateLevels(String orgId) {
        levels.clear();

        Map<String, ArstrinnResource> resources = getResources(orgId, ArstrinnResources.class)
                .collectMap(this::getSelfLink)
                .block();

        levels.put(orgId, resources);
    }

    public Map<String, UndervisningsgruppeResource> getTeachingGroups(String orgId) {
        Map<String, UndervisningsgruppeResource> resources = teachingGroups.get(orgId);

        if (resources == null || resources.size() == 0) {
            updateTeachingGroups(orgId);
        }

        return teachingGroups.get(orgId);
    }

    public void updateTeachingGroups(String orgId) {
        teachingGroups.clear();

        Map<String, UndervisningsgruppeResource> resources = getResources(orgId, UndervisningsgruppeResources.class)
                .collectMap(this::getSelfLink)
                .block();

        teachingGroups.put(orgId, resources);
    }

    public Map<String, FagResource> getSubjects(String orgId) {
        Map<String, FagResource> resources = subjects.get(orgId);

        if (resources == null || resources.size() == 0) {
            updateSubjects(orgId);
        }

        return subjects.get(orgId);
    }

    public void updateSubjects(String orgId) {
        subjects.clear();

        Map<String, FagResource> resources = getResources(orgId, FagResources.class)
                .collectMap(this::getSelfLink)
                .block();

        subjects.put(orgId, resources);
    }

    public <S, T extends AbstractCollectionResources<S>> Flux<S> getResources(String orgId, Class<T> clazz) {
        KulturtankenProperties.Organisation organisation = kulturtankenProperties.getOrganisations().get(orgId);

        return Flux.merge(organisation.getRegistration().values()
                .stream()
                .map(registration -> get(registration, clazz)
                        .flatMapIterable(T::getContent))
                .collect(Collectors.toList()));
    }

    public <T> Mono<T> get(KulturtankenProperties.Registration registration, Class<T> clazz) {
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

    private <T extends FintLinks> String getSelfLink(T resource) {
        return resource.getSelfLinks()
                .stream()
                .map(Link::getHref)
                .map(String::toLowerCase)
                .findFirst()
                .orElse(null);
    }

    private static final Map<Class<?>, String> paths = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>(SkoleResources.class, "/utdanning/utdanningsprogram/skole"),
            new AbstractMap.SimpleImmutableEntry<>(BasisgruppeResources.class, "/utdanning/elev/basisgruppe"),
            new AbstractMap.SimpleImmutableEntry<>(ArstrinnResources.class, "/utdanning/utdanningsprogram/arstrinn"),
            new AbstractMap.SimpleImmutableEntry<>(UndervisningsgruppeResources.class, "/utdanning/timeplan/undervisningsgruppe"),
            new AbstractMap.SimpleImmutableEntry<>(FagResources.class, "/utdanning/timeplan/fag"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}
