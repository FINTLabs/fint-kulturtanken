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

        //List<ArstrinnResource> arstrinn = arstrinnResources.getContent().stream().peek(System.out::println).collect(Collectors.toList());

        return fintService.getSkoleOrganisasjon(bearer);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity handelException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
