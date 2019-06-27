package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.*;
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
        OrganisasjonselementResources organisasjonselementResources = getOrganisasjonselementResources(bearer);
        Optional<OrganisasjonselementResource> topLevelOrg = getTopElement(organisasjonselementResources);
        topLevelOrg.ifPresent(o -> {
            schoolOrganisation.setNavn(o.getNavn());
            schoolOrganisation.setOrganisasjonsnummer(o.getOrganisasjonsnummer().getIdentifikatorverdi());
            schoolOrganisation.setKontaktinformasjon(getKontaktInformasjon(o.getKontaktinformasjon()));
        });
    }

    private List<Skole> getSkoleList(String bearer) {
        SkoleResources skoleResources = getSkoleResources(bearer);
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
        List<SkoleResource> schoolResourceList = getSkoleResources(bearer).getContent();
        List<ArstrinnResource> levelResourceList = getArstrinnResources(bearer).getContent();
        List<BasisgruppeResource> groupResourceList = getBasisgruppeResources(bearer).getContent();

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

    private OrganisasjonselementResources getOrganisasjonselementResources(String bearer) {
        return
                webClient.get()
                        .uri("/administrasjon/organisasjon/organisasjonselement")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .retrieve()
                        .bodyToMono(OrganisasjonselementResources.class)
                        .block();
    }

    private Optional<OrganisasjonselementResource> getTopElement(OrganisasjonselementResources organisasjonselementResources) {
        return organisasjonselementResources.getContent()
                .stream()
                .filter(it -> it.getSelfLinks().stream().anyMatch(l -> it.getOverordnet().stream().anyMatch(l::equals)))
                .findFirst();
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

    private Kontaktinformasjon getKontaktInformasjon(no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon contactInformation1) {
        Kontaktinformasjon contactInformation = new Kontaktinformasjon();
        contactInformation.setEpostadresse(contactInformation1.getEpostadresse());
        contactInformation.setMobiltelefonnummer(contactInformation1.getMobiltelefonnummer());
        return contactInformation;
    }
}
