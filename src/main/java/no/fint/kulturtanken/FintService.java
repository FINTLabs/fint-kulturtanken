package no.fint.kulturtanken;

import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.*;
import no.fint.model.resource.AbstractCollectionResources;
import no.fint.model.resource.Link;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResource;
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResources;
import no.fint.model.resource.utdanning.elev.BasisgruppeResource;
import no.fint.model.resource.utdanning.elev.BasisgruppeResources;
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResource;
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResources;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResource;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FintService {

    private final String GET_ORGANISATION_URI = "/administrasjon/organisasjon/organisasjonselement";
    private final String GET_SCHOOL_URI = "/utdanning/utdanningsprogram/skole";
    private final String GET_LEVEL_URI = "/utdanning/utdanningsprogram/arstrinn";
    private final String GET_GROUP_URI = "/utdanning/utdanningsprogram/basisgruppe";

    @Autowired
    private WebClient webClient;

    public SkoleOrganisasjon getSkoleOrganisasjon(String bearer) {
            return setUpSchoolOrganisation(bearer);
    }

    private SkoleOrganisasjon setUpSchoolOrganisation(String bearer) {
        SkoleOrganisasjon schoolOrganisation = new SkoleOrganisasjon();
        addOrganisationInfo(schoolOrganisation, bearer);
        schoolOrganisation.setSkole(getSkoleList(bearer));
        setSchoolLevelsAndGroups(schoolOrganisation, bearer);
        return schoolOrganisation;
    }

    private void addOrganisationInfo(SkoleOrganisasjon schoolOrganisation, String bearer) {
        OrganisasjonselementResources organisasjonselementResources = new OrganisasjonselementResources();
        organisasjonselementResources = (OrganisasjonselementResources) getResources(GET_ORGANISATION_URI, bearer, organisasjonselementResources);
        Optional<OrganisasjonselementResource> topLevelOrg = getTopElement(organisasjonselementResources);
        topLevelOrg.ifPresent(o -> {
            schoolOrganisation.setNavn(o.getNavn());
            schoolOrganisation.setOrganisasjonsnummer(o.getOrganisasjonsnummer().getIdentifikatorverdi());
            schoolOrganisation.setKontaktinformasjon(getKontaktInformasjon(o.getKontaktinformasjon()));
        });
    }

    private List<Skole> getSkoleList(String bearer) {
        SkoleResources skoleResources = new SkoleResources();
        skoleResources = (SkoleResources) getResources(GET_SCHOOL_URI, bearer, skoleResources);
        return
                skoleResources.getContent()
                        .stream()
                        .map(s -> {
                            Skole skole = new Skole();
                            skole.setNavn(s.getNavn());
                            skole.setSkolenummer(s.getSkolenummer().getIdentifikatorverdi());
                            skole.setKontaktinformasjon(getKontaktInformasjon(s.getKontaktinformasjon()));
                            skole.setOrganisasjonsnummer(s.getOrganisasjonsnummer().getIdentifikatorverdi());
                            return skole;
                        })
                        .collect(Collectors.toList());
    }

    private void setSchoolLevelsAndGroups(SkoleOrganisasjon schoolOrganisation, String bearer) {
        SkoleResources skoleResources = new SkoleResources();
        ArstrinnResources arstrinnResources = new ArstrinnResources();
        BasisgruppeResources basisgruppeResources = new BasisgruppeResources();
        List<SkoleResource> schoolResourceList = ((SkoleResources)(getResources(GET_SCHOOL_URI, bearer, skoleResources))).getContent();
        List<ArstrinnResource> levelResourceList = ((ArstrinnResources)(getResources(GET_LEVEL_URI, bearer, arstrinnResources))).getContent();
        List<BasisgruppeResource> groupResourceList = ((BasisgruppeResources)(getResources(GET_GROUP_URI, bearer, basisgruppeResources))).getContent();

        schoolResourceList.forEach(schoolResource -> {
            Link schoolResouceLink = schoolResource.getSelfLinks().get(0);
            filterLevelsAndGroups(levelResourceList, schoolResouceLink, groupResourceList, schoolOrganisation, schoolResource);
        });

    }

    private void filterLevelsAndGroups(List<ArstrinnResource> levelResourceList, Link schoolResouceLink, List<BasisgruppeResource> groupResourceList, SkoleOrganisasjon schoolOrganisation, SkoleResource schoolResource) {
        List<Trinn> levelList = new ArrayList<>();
        levelResourceList.forEach(level -> {
            Link levelSelfLink = level.getSelfLinks().get(0);
            List<Basisgrupper> filteredGroups = filterGroups(levelSelfLink, schoolResouceLink, groupResourceList);
            addLvlAndGroupToSchool(level, levelList, filteredGroups, schoolOrganisation, schoolResource);
        });
    }

    private List<Basisgrupper> filterGroups(Link levelSelfLink, Link schoolResouceLink, List<BasisgruppeResource> groupResources) {
        return groupResources.stream()
                .filter(groupResource ->
                        groupResource.getTrinn().get(0).equals(levelSelfLink))
                .filter(levelFilteredGroupResource ->
                        levelFilteredGroupResource.getSkole().get(0).equals(schoolResouceLink))
                .map(filteredGroupResource -> {
                    Basisgrupper basisgrupper = new Basisgrupper();
                    basisgrupper.setNavn(filteredGroupResource.getNavn());
                    basisgrupper.setAntall(filteredGroupResource.getElevforhold().size());
                    return basisgrupper;
                }).collect(Collectors.toList());
    }

    private void addLvlAndGroupToSchool(ArstrinnResource level, List<Trinn> levelList, List<Basisgrupper> filteredGroups, SkoleOrganisasjon schoolOrganisation, SkoleResource skoleResource) {
        Trinn trinn = new Trinn();
        trinn.setNiva(level.getNavn());
        trinn.setBasisgrupper(filteredGroups);
        if (filteredGroups.size() > 0)
            levelList.add(trinn);
        for (Skole skole : schoolOrganisation.getSkole()) {
            if (skole.getNavn().equals(skoleResource.getNavn())) {
                skole.setTrinn(levelList);
            }
        }
    }

    private AbstractCollectionResources getResources(String uri, String bearer, AbstractCollectionResources resourceClass){
        return
                webClient.get()
                        .uri(uri)
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .retrieve()
                        .bodyToMono(resourceClass.getClass())
                        .onErrorResume(response -> {
                            if (response instanceof WebClientResponseException) {
                                WebClientResponseException response1 = (WebClientResponseException) response;
                                if (response1.getStatusCode() == HttpStatus.NOT_FOUND) {
                                    return Mono.error(new URINotFoundException(response1.getStatusText()));
                                }
                                if (response1.getStatusCode() == HttpStatus.REQUEST_TIMEOUT) {
                                    return Mono.error(new ResourceRequestTimeoutException(response1.getStatusText()));
                                }
                            }
                            if (response instanceof ReadTimeoutException) {
                                return Mono.error(new ResourceRequestTimeoutException(response.getMessage()));
                            }
                            return Mono.error(new UnableToCreateResourceException(response.getMessage()));
                        })
                        .block();

    }

    private Optional<OrganisasjonselementResource> getTopElement(OrganisasjonselementResources organisasjonselementResources) {
        return organisasjonselementResources.getContent()
                .stream()
                .filter(it -> it.getSelfLinks().stream().anyMatch(l -> it.getOverordnet().stream().anyMatch(l::equals)))
                .findFirst();
    }

    private Kontaktinformasjon getKontaktInformasjon(no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon contactInformation1) {
        Kontaktinformasjon contactInformation = new Kontaktinformasjon();
        contactInformation.setEpostadresse(contactInformation1.getEpostadresse());
        contactInformation.setMobiltelefonnummer(contactInformation1.getMobiltelefonnummer());
        return contactInformation;
    }
}
