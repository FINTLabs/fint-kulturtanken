package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.SkoleOrganisasjon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import javax.validation.constraints.NotBlank;

@Slf4j
@RestController
@RequestMapping("api/skoleorganisasjon")
public class Controller {

    @Autowired
    private WebClient webClient;

    @Autowired
    private FintService fintService;

    @GetMapping("{id}")
    public SkoleOrganisasjon getSkoleOrganisasjonById(@PathVariable String id,
                                                      @RequestHeader(name = HttpHeaders.AUTHORIZATION) @NotBlank String bearer
    ) {
        log.info("Id: {}", id);
        log.info("Bearer token: {}", bearer);

        return fintService.getSkoleOrganisasjon(bearer);
    }

    @ExceptionHandler(ResourceRequestTimeoutException.class)
    public ResponseEntity handleTimeOutException(ResourceRequestTimeoutException e) {
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
    }
    @ExceptionHandler(UnableToCreateResourceException.class)
    public ResponseEntity handleCreateResourceFailed(UnableToCreateResourceException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    @ExceptionHandler(URINotFoundException.class)
    public ResponseEntity handleWrongURI(URINotFoundException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
