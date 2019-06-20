package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.Kontaktinformasjon;
import no.fint.kulturtanken.model.Skole;
import no.fint.kulturtanken.model.Skoleeier;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResources;
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResource;
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResources;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("api/skoleeier")
public class Controller {

    @Autowired
    private WebClient webClient;

    @GetMapping("{id}")
    public Skoleeier getSkoleeierById(@PathVariable String id,
                                      @RequestHeader(name = HttpHeaders.AUTHORIZATION) @NotBlank String bearer
    ) {
        log.info("Id: {}", id);
        log.info("Bearer token: {}", bearer);

        OrganisasjonselementResources organisasjonselementResources = webClient.get()
                .uri("/administrasjon/organisasjon/organisasjonselement")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .retrieve()
                .bodyToMono(OrganisasjonselementResources.class)
                .block();

        Skoleeier skoleeier = new Skoleeier();

        Optional<OrganisasjonselementResource> overordnet = organisasjonselementResources.getContent().stream().filter(it -> it.getSelfLinks().stream().anyMatch(l -> it.getOverordnet().stream().anyMatch(l::equals))).findFirst();
        overordnet.ifPresent(o -> {
            skoleeier.setNavn(o.getNavn());
            skoleeier.setOrganisasjonsnummer(o.getOrganisasjonsnummer().getIdentifikatorverdi());
            skoleeier.setKontaktinformasjon(getKontaktinformasjon(o.getKontaktinformasjon()));
        });

        SkoleResources skoleResources = webClient.get()
                .uri("/utdanning/utdanningsprogram/skole")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .retrieve()
                .bodyToMono(SkoleResources.class)
                .block();

        skoleeier.setSkole(
        skoleResources.getContent()
                .stream()
                .map(s -> {
                    Skole skole = new Skole();
                    skole.setNavn(s.getNavn());
                    skole.setSkolenummer(s.getSkolenummer().getIdentifikatorverdi());
                    skole.setKontaktinformasjon(getKontaktinformasjon(s.getKontaktinformasjon()));
                    skole.setOrganisasjonsnummer(s.getOrganisasjonsnummer().getIdentifikatorverdi());
                    return skole;
                })
                .collect(Collectors.toList()));

        ArstrinnResources arstrinnResources = webClient.get()
                .uri("/utdanning/utdanningsprogram/arstrinn")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .retrieve()
                .bodyToMono(ArstrinnResources.class)
                .block();

        //List<ArstrinnResource> arstrinn = arstrinnResources.getContent().stream().peek(System.out::println).collect(Collectors.toList());

        return skoleeier;
    }

    private Kontaktinformasjon getKontaktinformasjon(no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon kontaktinformasjon1) {
        Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();
        kontaktinformasjon.setEpostadresse(kontaktinformasjon1.getEpostadresse());
        kontaktinformasjon.setMobiltelefonnummer(kontaktinformasjon1.getMobiltelefonnummer());
        return kontaktinformasjon;
    }
}
