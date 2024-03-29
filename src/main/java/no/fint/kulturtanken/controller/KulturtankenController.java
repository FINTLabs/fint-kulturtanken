package no.fint.kulturtanken.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.configuration.KulturtankenProperties;
import no.fint.kulturtanken.exception.SchoolOwnerNotFoundException;
import no.fint.kulturtanken.model.Skoleeier;
import no.fint.kulturtanken.service.KulturtankenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Comparator;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/skoleeier")
@RequiredArgsConstructor
public class KulturtankenController {

    private final KulturtankenService kulturtankenService;
    private final KulturtankenProperties kulturtankenProperties;

    @GetMapping("/{orgId}")
    public Skoleeier getSchoolOwner(@PathVariable String orgId) {
        log.info(orgId);
        if (kulturtankenProperties.getOrganisations().containsKey(orgId)) {
            return kulturtankenService.getSchoolOwner(orgId);
        } else {
            throw new SchoolOwnerNotFoundException(String.format("School owner not found for organisation number: %s", orgId));
        }
    }

    @GetMapping
    public Stream<KulturtankenProperties.Organisation> getOrganisations() {
        return kulturtankenProperties.getOrganisations().entrySet()
                .stream()
                .map(organisation -> {
                    organisation.getValue().setUri(ServletUriComponentsBuilder.fromCurrentContextPath()
                            .pathSegment("skoleeier", organisation.getKey()).build().toUri());
                    return organisation.getValue();
                }).sorted(Comparator.comparing(KulturtankenProperties.Organisation::getName));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> handleRestClientResponseException(RestClientResponseException ex) {
        log.error("RestClientException - Status: {}, Body: {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<String> handleWebClientResponseException(WebClientResponseException ex) {
        log.error("WebClientException - Status: {}, Body: {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
    }
}
