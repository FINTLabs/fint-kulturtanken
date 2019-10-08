package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.Skoleeier;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

@Slf4j
@RestController
@RequestMapping("/skoleeier")
public class KulturtankenController {

    private final KulturtankenService kulturtankenService;

    public KulturtankenController(KulturtankenService kulturtankenService) {
        this.kulturtankenService = kulturtankenService;
    }

    @GetMapping
    public Skoleeier getSchoolOwner(@RequestHeader(name = HttpHeaders.AUTHORIZATION) @NotBlank String bearer) {
        log.info("Bearer token: {}", bearer);

        return kulturtankenService.getSchoolOwner(bearer);
    }

    /*
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> handleRestClientResponseException(RestClientResponseException ex) {
        log.error("Error from RestClient - Status {}, Body {}", ex.getRawStatusCode(), ex.getResponseBodyAsString(), ex);
        return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
    }
    */
}
