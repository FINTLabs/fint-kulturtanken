package no.fint.kulturtanken;

import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.Exceptions.ResourceRequestTimeoutException;
import no.fint.kulturtanken.Exceptions.URINotFoundException;
import no.fint.kulturtanken.Exceptions.UnableToCreateResourceException;
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
public class KulturtankenService {

    private final String GET_ORGANISATION_URI = "/administrasjon/organisasjon/organisasjonselement";
    private final String GET_SCHOOL_URI = "/utdanning/utdanningsprogram/skole";
    private final String GET_LEVEL_URI = "/utdanning/utdanningsprogram/arstrinn";
    private final String GET_GROUP_URI = "/utdanning/utdanningsprogram/basisgruppe";

    @Autowired
    private WebClient webClient;

    public SkoleOrganisasjon getSkoleOrganisasjon(String bearer) {
        SkoleOrganisasjon skoleOrganisasjon = new SkoleOrganisasjon();
        addOrganisationInfo(skoleOrganisasjon, bearer);
        skoleOrganisasjon.setSkole(getSkoleList(bearer));
        if (skoleOrganisasjon.getSkole().size() > 0)
        setSchoolLevelsAndGroups(skoleOrganisasjon, bearer);
        return skoleOrganisasjon;
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
        try{
            skoleResources = (SkoleResources) getResources(GET_SCHOOL_URI, bearer, skoleResources);
        }catch (URINotFoundException | ResourceRequestTimeoutException | UnableToCreateResourceException e){
            log.error(e.getMessage());
            skoleResources = new SkoleResources();
        }
        if (skoleResources.getContent().isEmpty() || (skoleResources.getContent().size()==1 && skoleResources.getContent().get(0).getNavn()==null)){
            return new ArrayList<>();
        }
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
        List<SkoleResource> schoolResourceList;
        List<ArstrinnResource> levelResourceList;
        List<BasisgruppeResource> groupResourceList;
        try{
            schoolResourceList = ((SkoleResources) (getResources(GET_SCHOOL_URI, bearer, skoleResources))).getContent();
            levelResourceList = ((ArstrinnResources) (getResources(GET_LEVEL_URI, bearer, arstrinnResources))).getContent();
        }catch (URINotFoundException | ResourceRequestTimeoutException | UnableToCreateResourceException e){
            return;
        }
        if (levelResourceList.isEmpty() || (levelResourceList.size()==1 && levelResourceList.get(0).getNavn()==null)){
            return;
        }
        try{
            groupResourceList = ((BasisgruppeResources) (getResources(GET_GROUP_URI, bearer, basisgruppeResources))).getContent();
        }catch (URINotFoundException | ResourceRequestTimeoutException | UnableToCreateResourceException e){
            groupResourceList = new ArrayList<>();
        }

        List<BasisgruppeResource> finalGroupResourceList = groupResourceList;
        schoolResourceList.forEach(schoolResource -> {
            Link schoolResouceLink = schoolResource.getSelfLinks().get(0);
            String schoolName = schoolResource.getNavn();
            filterLevelsAndGroups(levelResourceList, schoolResouceLink, finalGroupResourceList, schoolOrganisation, schoolName);
        });

    }

    private void filterLevelsAndGroups(List<ArstrinnResource> levelResourceList, Link schoolResourceLink, List<BasisgruppeResource> groupResourceList, SkoleOrganisasjon schoolOrganisation, String schoolName) {
        List<Trinn> levelList = new ArrayList<>();
        levelResourceList.forEach(level -> {
            Link levelSelfLink = level.getSelfLinks().get(0);
            List<Basisgrupper> filteredGroups =
                    (groupResourceList.isEmpty() || (groupResourceList.size()==1 && groupResourceList.get(0).getTrinn()==null))
                            ?
                            new ArrayList<>()
                            :
                            filterGroups(levelSelfLink, schoolResourceLink, groupResourceList);
            addLevelAndGroupToSchool(level, levelList, filteredGroups, schoolOrganisation, schoolName);
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

    private void addLevelAndGroupToSchool(ArstrinnResource level, List<Trinn> levelList, List<Basisgrupper> filteredGroups, SkoleOrganisasjon schoolOrganisation, String schoolName) {
        Trinn trinn = new Trinn();
        trinn.setNiva(level.getNavn());
        trinn.setBasisgrupper(filteredGroups);
        if (filteredGroups.size() > 0)
            levelList.add(trinn);
        for (Skole skole : schoolOrganisation.getSkole()) {
            if (skole.getNavn().equals(schoolName)) {
                skole.setTrinn(levelList);
            }
        }
    }

    private AbstractCollectionResources getResources(String uri, String bearer, AbstractCollectionResources resourceClass) {
        return
                webClient.get()
                        .uri(uri)
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .retrieve()
                        .bodyToMono(resourceClass.getClass())
                        .onErrorResume(this::handleWebClientErrors)
                        .block();

    }

    private Mono handleWebClientErrors(Throwable response) {
        if (response instanceof WebClientResponseException) {
            WebClientResponseException clientResponseException = (WebClientResponseException) response;
            if (clientResponseException.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Mono.error(new URINotFoundException(clientResponseException.getStatusText()));
            }
            if (clientResponseException.getStatusCode() == HttpStatus.REQUEST_TIMEOUT) {
                return Mono.error(new ResourceRequestTimeoutException(clientResponseException.getStatusText()));
            }
        }
        if (response instanceof ReadTimeoutException) {
            return Mono.error(new ResourceRequestTimeoutException(response.getMessage()));
        }
        return Mono.error(new UnableToCreateResourceException(response.getMessage()));
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
