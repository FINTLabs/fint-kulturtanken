package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.Skoleeier;
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

    @GetMapping
    public Skoleeier getSchoolOwner() {
        return kulturtankenService.getSchoolOwner();
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> handleRestClientResponseException(RestClientResponseException ex) {
        log.error("RestClientException - Status: {}, Body: {}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
    }
}
