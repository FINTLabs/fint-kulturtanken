package no.fint.kulturtanken.controller;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.Skoleeier;
import no.fint.kulturtanken.service.KulturtankenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@RestController
@RequestMapping("/skoleeier")
public class KulturtankenController {

    private final KulturtankenService kulturtankenService;

    public KulturtankenController(KulturtankenService kulturtankenService) {
        this.kulturtankenService = kulturtankenService;
    }

    @GetMapping(value = {"", "{orgId}"})
    public Skoleeier getSchoolOwner(@PathVariable(required = false) String orgId) {
        return (orgId == null ? kulturtankenService.getSchoolOwner("971045698") :
                kulturtankenService.getSchoolOwner(orgId));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> handleRestClientResponseException(RestClientResponseException ex) {
        log.error("RestClientException - Status: {}, Body: {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
    }
}
