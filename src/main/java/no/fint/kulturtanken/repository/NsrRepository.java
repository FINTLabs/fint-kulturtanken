package no.fint.kulturtanken.repository;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.Enhet;
import no.fint.kulturtanken.model.Skole;
import no.fint.kulturtanken.model.Skoleeier;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Predicate;

/**
 * NSR means "Nasjonalt Skoleregister"
 */
@Slf4j
@Repository
public class NsrRepository {

    private final WebClient webClient;

    private final MultiValueMap<String, Skole> schools = new LinkedMultiValueMap<>();
    private final Map<String, Skoleeier> schoolOwners = new HashMap<>();

    public NsrRepository(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://data-nsr.udir.no/enhet")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1))
                .build();
    }

    public List<Skole> getSchools(String orgId) {
        return schools.getOrDefault(orgId, Collections.emptyList());
    }

    public void updateSchools(String orgId) {
        getUnit(orgId)
                .flatMapIterable(Enhet::getChildRelasjoner)
                .map(Enhet.ChildRelasjon::getEnhet)
                .map(Enhet::getOrgNr)
                .distinct()
                .flatMap(this::getUnit)
                .filter(isValidUnit)
                .map(Skole::fromNsr)
                .toStream()
                .forEach(school -> schools.add(orgId, school));
    }

    public Skoleeier getSchoolOwnerById(String orgId) {
        return Optional.ofNullable(schoolOwners.get(orgId)).orElse(null);
    }

    public void updateSchoolOwner(String orgId) {
        Skoleeier resource = getUnit(orgId)
                .map(Skoleeier::fromNsr)
                .block();

        schoolOwners.put(orgId, resource);
    }

    public Mono<Enhet> getUnit(String orgId) {
        return webClient.get()
                .uri("/{orgId}", orgId)
                .retrieve()
                .bodyToMono(Enhet.class);
    }

    public void updateResources(String orgId) {
        schools.put(orgId, new ArrayList<>());

        updateSchoolOwner(orgId);
        updateSchools(orgId);
    }

    private final Predicate<Enhet> isValidUnit = unit ->
            unit.getErOffentligSkole() && unit.getErVideregaaendeSkole() && unit.getErAktiv();
}
