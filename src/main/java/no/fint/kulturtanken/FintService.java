package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.*;
import no.fint.model.resource.Link;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResources;
import no.fint.model.resource.utdanning.elev.BasisgruppeResource;
import no.fint.model.resource.utdanning.elev.BasisgruppeResources;
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResources;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources;
import no.fint.model.utdanning.elev.Basisgruppe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FintService {

    @Autowired
    private WebClient webClient;


    public SkoleOrganisasjon getSkoleOrganisasjon(String bearer) {
        SkoleOrganisasjon skoleOrganisasjon = new SkoleOrganisasjon();
        OrganisasjonselementResources organisasjonselementResources = getOrganisasjonselementResources(bearer);
        Optional<OrganisasjonselementResource> overordnet = getOverordnet(organisasjonselementResources);
        overordnet.ifPresent(o -> {
            skoleOrganisasjon.setNavn(o.getNavn());
            skoleOrganisasjon.setOrganisasjonsnummer(o.getOrganisasjonsnummer().getIdentifikatorverdi());
            skoleOrganisasjon.setKontaktinformasjon(getKontaktinformasjon(o.getKontaktinformasjon()));
        });
        SkoleResources skoleResources = getSkoleResources(bearer);

        skoleOrganisasjon.setSkole(
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

        ArstrinnResources arstrinnResources = getArstrinnResources(bearer);
        BasisgruppeResources basisgruppeResources = getBasisgruppeResources(bearer);

        for (Skole skole : skoleOrganisasjon.getSkole()) {
            Basisgrupper basisgrupper = new Basisgrupper();
            arstrinnResources.getContent().stream().map(a -> {
                Trinn trinn = new Trinn();
                trinn.setNiva(a.getNavn());
                trinn.setBasisgrupper();
                List<Link> selfLinks = a.getSelfLinks();

                List<BasisgruppeResource> basisgruppeResourceList = basisgruppeResources.getContent().stream().filter(basisgruppe -> basisgruppe.getTrinn().stream().anyMatch(link -> selfLinks.stream().anyMatch(link::equals))).collect(Collectors.toList());
                List<Basisgruppe> basisgruppeList = new ArrayList<>();
                for (BasisgruppeResource br : basisgruppeResourceList){
                    Basisgrupper b = new Basisgrupper();
                    b.setNavn(br.getNavn());
                    b.setAntall(br.get);
                }
            }


        }


        return skoleOrganisasjon;
    }

    private OrganisasjonselementResources getOrganisasjonselementResources(String bearer) {
        return
                webClient.get()
                        .uri("/administrasjon/organisasjon/organisasjonselement")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .retrieve()
                        .bodyToMono(OrganisasjonselementResources.class)
                        .block();
    }

    private Optional<OrganisasjonselementResource> getOverordnet(OrganisasjonselementResources organisasjonselementResources) {
        return organisasjonselementResources.getContent().stream().filter(it -> it.getSelfLinks().stream().anyMatch(l -> it.getOverordnet().stream().anyMatch(l::equals))).findFirst();
    }

    private SkoleResources getSkoleResources(String bearer) {
        return webClient.get()
                .uri("/utdanning/utdanningsprogram/skole")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .retrieve()
                .bodyToMono(SkoleResources.class)
                .block();
    }

    private ArstrinnResources getArstrinnResources(String bearer) {
        return webClient.get()
                .uri("/utdanning/utdanningsprogram/arstrinn")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .retrieve()
                .bodyToMono(ArstrinnResources.class)
                .block();
    }

    private BasisgruppeResources getBasisgruppeResources(String bearer) {
        return webClient.get()
                .uri("/utdanning/utdanningsprogram/basisgruppe")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .retrieve()
                .bodyToMono(BasisgruppeResources.class)
                .block();
    }

    private Kontaktinformasjon getKontaktinformasjon(no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon kontaktinformasjon1) {
        Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();
        kontaktinformasjon.setEpostadresse(kontaktinformasjon1.getEpostadresse());
        kontaktinformasjon.setMobiltelefonnummer(kontaktinformasjon1.getMobiltelefonnummer());
        return kontaktinformasjon;
    }
}
