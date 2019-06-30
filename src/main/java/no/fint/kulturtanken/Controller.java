package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.SkoleOrganisasjon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

@Slf4j
@RestController
@RequestMapping("api/skoleorganisasjon")
public class Controller {

    @Autowired
    private FintService fintService;

    @GetMapping
    public SkoleOrganisasjon getSkoleOrganisasjon(@RequestHeader(name = HttpHeaders.AUTHORIZATION) @NotBlank String bearer) {
        log.info("Bearer token: {}", bearer);

        return fintService.getSkoleOrganisasjon(bearer);
    }

    @ExceptionHandler(ResourceRequestTimeoutException.class)
    public ResponseEntity handleTimeOutException(Exception e) {
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
    }

    @ExceptionHandler(UnableToCreateResourceException.class)
    public ResponseEntity handleCreateResourceFailed(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(URINotFoundException.class)
    public ResponseEntity handleNotFound(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
