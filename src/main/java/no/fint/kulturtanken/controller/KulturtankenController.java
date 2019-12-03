package no.fint.kulturtanken.controller;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.config.KulturtankenProperties;
import no.fint.kulturtanken.exception.SchoolOwnerNotFoundException;
import no.fint.kulturtanken.model.Organisasjon;
import no.fint.kulturtanken.model.Skoleeier;
import no.fint.kulturtanken.service.KulturtankenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/skoleeier")
public class KulturtankenController {

    private final KulturtankenService kulturtankenService;
    private final KulturtankenProperties kulturtankenProperties;

    public KulturtankenController(KulturtankenService kulturtankenService, KulturtankenProperties kulturtankenProperties) {
        this.kulturtankenService = kulturtankenService;
        this.kulturtankenProperties = kulturtankenProperties;
    }

    @GetMapping("/{orgId}")
    public Skoleeier getSchoolOwner(@PathVariable String orgId) {
        if (kulturtankenProperties.getOrganisations().containsKey(orgId))
            return kulturtankenService.getSchoolOwner(orgId);
        else
            throw new SchoolOwnerNotFoundException(String.format("School owner not found for organisation number: %s", orgId));
    }

    @GetMapping("/")
    public List<Organisasjon> getOrganisations() {
        List<Organisasjon> organisations = new  ArrayList<>();

        kulturtankenProperties.getOrganisations().forEach((key, value) -> {
            Organisasjon organisation = new Organisasjon();
            organisation.setNavn(value.getName());
            organisation.setOrganisasjonsnummer(key);
            organisation.setGrupper(value.getGroups());
            organisation.setUri(ServletUriComponentsBuilder.fromCurrentContextPath().pathSegment("skoleeier", key).build().toUri());
            organisations.add(organisation);
        });

        organisations.sort(Comparator.comparing(Organisasjon::getNavn));

        return organisations;
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> handleRestClientResponseException(RestClientResponseException ex) {
        log.error("RestClientException - Status: {}, Body: {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
    }
}
