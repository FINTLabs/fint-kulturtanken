package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;
import no.fint.kulturtanken.model.*;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
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

import java.util.*;
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
        schoolOrganisation.setSkole(getSchool(bearer));
        setSchoolLevelsAndGroups(schoolOrganisation, bearer);
        return schoolOrganisation;
    }


    private void addOrganisationInfo(SkoleOrganisasjon schoolOrganisation, String bearer) {
        OrganisasjonselementResources organisasjonselementResources = getOrganisasjonselementResources(bearer);
        Optional<OrganisasjonselementResource> topLevelOrg = getTopElement(organisasjonselementResources);
        topLevelOrg.ifPresent(o -> {
            schoolOrganisation.setNavn(o.getNavn());
            schoolOrganisation.setOrganisasjonsnummer(o.getOrganisasjonsnummer().getIdentifikatorverdi());
            schoolOrganisation.setKontaktinformasjon(getKontaktinformasjon(o.getKontaktinformasjon()));
        });
    }

    private List<Skole> getSchool(String bearer) {
        SkoleResources skoleResources = getSkoleResources(bearer);
        return
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
        levelResourceList.forEach(level -> {
            Link levelSelfLink = level.getSelfLinks().get(0);
            List<Basisgrupper> filteredGroups = filterGroups(levelSelfLink, schoolResouceLink, groupResourceList);
            addLvlAndGroupToSchool(level, filteredGroups, schoolOrganisation, schoolResource);
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

    private void addLvlAndGroupToSchool(ArstrinnResource level, List<Basisgrupper> filteredGroups, SkoleOrganisasjon schoolOrganisation, SkoleResource skoleResource) {
        List<Trinn> trinnList = new ArrayList<>();
        Trinn trinn = new Trinn();
        trinn.setNiva(level.getNavn());
        trinn.setBasisgrupper(filteredGroups);
        if (filteredGroups.size() > 0)
            trinnList.add(trinn);
        for (Skole skole : schoolOrganisation.getSkole()) {
            if (skole.getNavn().equals(skoleResource.getNavn())) {
                skole.setTrinn(trinnList);
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

    private Kontaktinformasjon getKontaktinformasjon(no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon kontaktinformasjon1) {
        Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon();
        kontaktinformasjon.setEpostadresse(kontaktinformasjon1.getEpostadresse());
        kontaktinformasjon.setMobiltelefonnummer(kontaktinformasjon1.getMobiltelefonnummer());
        return kontaktinformasjon;
    }

    public SkoleOrganisasjon test() {
        Identifikator identifikatorOrgNummer = new Identifikator();
        identifikatorOrgNummer.setIdentifikatorverdi("98765432");
        Identifikator identifikatorSkoleNummer = new Identifikator();
        identifikatorSkoleNummer.setIdentifikatorverdi("12345");
        no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon kontaktinformasjon = new no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon();
        kontaktinformasjon.setEpostadresse("Ola@test.no");
        kontaktinformasjon.setMobiltelefonnummer("47759931");
        Map<String, List<Link>> linkListVg1 = new TreeMap<>();
        Map<String, List<Link>> linkListVg2 = new TreeMap<>();
        Map<String, List<Link>> linkListVg3 = new TreeMap<>();
        Map<String, List<Link>> linkListVg4IkkeISkole = new TreeMap<>();
        Map<String, List<Link>> linkListSkole = new TreeMap<>();
        Map<String, List<Link>> linkListSkole2 = new TreeMap<>();
        Map<String, List<Link>> linkListForbasisGruppeVG1 = new TreeMap<>();
        Map<String, List<Link>> linkListForbasisGruppeVG2 = new TreeMap<>();
        Map<String, List<Link>> linkListForbasisGruppeVG3 = new TreeMap<>();
        Map<String, List<Link>> linkListForbasisGruppeVG1SomIkkeErISkole = new TreeMap<>();
        List<Link> listVG1 = new ArrayList<>();
        List<Link> listVG2 = new ArrayList<>();
        List<Link> listVG3 = new ArrayList<>();
        List<Link> listVG4IkkeISkole = new ArrayList<>();
        List<Link> listSkole = new ArrayList<>();
        List<Link> listSkole2 = new ArrayList<>();
        List<Link> listForBasisGruppeSomIkkeErISkole = new ArrayList<>();
        List<Link> listMedAntallStudenter = new ArrayList<>();

        listVG1.add(new Link("http://ola.test.link.vg1/"));
        linkListVg1.put("self", listVG1);

        listVG2.add(new Link("http://ola.test.link.vg2/"));
        linkListVg2.put("self", listVG2);

        listVG3.add(new Link("http://ola.test.link.vg3/"));
        linkListVg3.put("self", listVG3);

        listVG4IkkeISkole.add(new Link("http://ola.test.link.vg4.ikke.i.skole/"));
        linkListVg4IkkeISkole.put("self", listVG4IkkeISkole);

        listSkole.add(new Link("http://bo.barneskole/"));
        listSkole2.add(new Link("http://bo.folkestad/"));
        listMedAntallStudenter.add(new Link("Per"));
        listMedAntallStudenter.add(new Link("Gunnar"));
        listMedAntallStudenter.add(new Link("Knut"));
        listMedAntallStudenter.add(new Link("Lars"));

        linkListForbasisGruppeVG1.put("trinn", listVG1);
        linkListForbasisGruppeVG2.put("trinn", listVG2);
        linkListForbasisGruppeVG3.put("trinn", listVG3);
        linkListForbasisGruppeVG1.put("skole", listSkole);
        linkListForbasisGruppeVG2.put("skole", listSkole);
        linkListForbasisGruppeVG3.put("skole", listSkole);
        linkListForbasisGruppeVG1.put("elevforhold", listMedAntallStudenter);
        linkListForbasisGruppeVG2.put("elevforhold", listMedAntallStudenter);
        linkListForbasisGruppeVG3.put("elevforhold", listMedAntallStudenter);

        listForBasisGruppeSomIkkeErISkole.add(new Link("http://basisgrupp.ikke.med.i.skole"));
        linkListForbasisGruppeVG1SomIkkeErISkole.put("trinn", listVG1);
        linkListForbasisGruppeVG1SomIkkeErISkole.put("skole", listForBasisGruppeSomIkkeErISkole);
        linkListForbasisGruppeVG1SomIkkeErISkole.put("elevforhold", listMedAntallStudenter);

        OrganisasjonselementResource organisasjonselementResource = new OrganisasjonselementResource();
        organisasjonselementResource.setNavn("Telemark Fylkeskommune");
        organisasjonselementResource.setOrganisasjonsnummer(identifikatorOrgNummer);
        organisasjonselementResource.setKontaktinformasjon(kontaktinformasjon);

        SkoleResource skoleResource = new SkoleResource();
        skoleResource.setNavn("Bø Barneskule");
        skoleResource.setSkolenummer(identifikatorSkoleNummer);
        skoleResource.setOrganisasjonsnummer(identifikatorOrgNummer);
        skoleResource.setKontaktinformasjon(kontaktinformasjon);
        SkoleResource skoleResource2 = new SkoleResource();
        skoleResource2.setNavn("Folkestad skule");
        skoleResource2.setSkolenummer(identifikatorSkoleNummer);
        skoleResource2.setOrganisasjonsnummer(identifikatorOrgNummer);
        skoleResource2.setKontaktinformasjon(kontaktinformasjon);
        SkoleResources skoleResources = new SkoleResources();
        skoleResources.addResource(skoleResource);
        skoleResources.addResource(skoleResource2);
        linkListSkole.put("self", listSkole);
        linkListSkole2.put("self", listSkole2);
        skoleResource.setLinks(linkListSkole);
        skoleResource2.setLinks(linkListSkole2);

        ArstrinnResources arstrinnResources = new ArstrinnResources();
        BasisgruppeResources basisgruppeResources = new BasisgruppeResources();

        ArstrinnResource arstrinnResource = new ArstrinnResource();
        ArstrinnResource arstrinnResource2 = new ArstrinnResource();
        ArstrinnResource arstrinnResource3 = new ArstrinnResource();
        ArstrinnResource arstrinnResource4 = new ArstrinnResource();
        arstrinnResource.setNavn("VG1");
        arstrinnResource.setLinks(linkListVg1);
        arstrinnResource2.setNavn("VG2");
        arstrinnResource2.setLinks(linkListVg2);
        arstrinnResource3.setNavn("VG3");
        arstrinnResource3.setLinks(linkListVg3);
        arstrinnResource4.setNavn("VGAnnenLink");
        arstrinnResource4.setLinks(linkListVg4IkkeISkole);
        arstrinnResources.addResource(arstrinnResource);
        arstrinnResources.addResource(arstrinnResource2);
        arstrinnResources.addResource(arstrinnResource3);
        arstrinnResources.addResource(arstrinnResource4);


        BasisgruppeResource basisgruppeResource = new BasisgruppeResource();
        BasisgruppeResource basisgruppeResource2 = new BasisgruppeResource();
        BasisgruppeResource basisgruppeResource3 = new BasisgruppeResource();
        BasisgruppeResource basisgruppeResource4 = new BasisgruppeResource();
        BasisgruppeResource basisgruppeResource5 = new BasisgruppeResource();
        basisgruppeResource.setLinks(linkListForbasisGruppeVG1);
        basisgruppeResource2.setLinks(linkListForbasisGruppeVG2);
        basisgruppeResource3.setLinks(linkListForbasisGruppeVG3);
        basisgruppeResource4.setLinks(linkListForbasisGruppeVG1);
        basisgruppeResource5.setLinks(linkListForbasisGruppeVG1SomIkkeErISkole);
        basisgruppeResource.setNavn("VG1 - Idrett");
        basisgruppeResource2.setNavn("VG2 - Fotball");
        basisgruppeResource3.setNavn("VG3 - Håndball");
        basisgruppeResource4.setNavn("VG1 - Almenn");
        basisgruppeResource5.setNavn("VG1 - Frisør - Ikke del av Bø barneskole");
        basisgruppeResources.addResource(basisgruppeResource);
        basisgruppeResources.addResource(basisgruppeResource2);
        basisgruppeResources.addResource(basisgruppeResource3);
        basisgruppeResources.addResource(basisgruppeResource4);
        basisgruppeResources.addResource(basisgruppeResource5);

        SkoleOrganisasjon skoleOrganisasjon = new SkoleOrganisasjon();
        skoleOrganisasjon.setNavn(organisasjonselementResource.getNavn());
        skoleOrganisasjon.setOrganisasjonsnummer(organisasjonselementResource.getOrganisasjonsnummer().getIdentifikatorverdi());
        skoleOrganisasjon.setKontaktinformasjon(getKontaktinformasjon(organisasjonselementResource.getKontaktinformasjon()));

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


        for (SkoleResource skoleResourceTest : skoleResources.getContent()) {
            Link skoleResouceLink = skoleResourceTest.getSelfLinks().get(0);
            List<Trinn> trinnList = new ArrayList<>();
            arstrinnResources.getContent().forEach(aarstrinn -> {
                Trinn trinn = new Trinn();
                trinn.setNiva(aarstrinn.getNavn());
                List<Link> aarstrinnSelfLinks = aarstrinn.getSelfLinks();
                List<Basisgrupper> basisgruppeList = new ArrayList<>();
                for (BasisgruppeResource basisgruppeResourceTest : basisgruppeResources.getContent()) {
                    if (basisgruppeResourceTest.getTrinn().get(0).equals(aarstrinnSelfLinks.get(0))) {
                        if (basisgruppeResourceTest.getSkole().get(0).equals(skoleResouceLink)) {
                            Basisgrupper basisgrupper = new Basisgrupper();
                            basisgrupper.setNavn(basisgruppeResourceTest.getNavn());
                            basisgrupper.setAntall(basisgruppeResourceTest.getElevforhold().size());
                            basisgruppeList.add(basisgrupper);
                        }
                    }
                }
                trinn.setBasisgrupper(basisgruppeList);
                if (basisgruppeList.size() > 0)
                    trinnList.add(trinn);
                for (Skole skole : skoleOrganisasjon.getSkole()) {
                    if (skole.getNavn().equals(skoleResourceTest.getNavn())) {
                        skole.setTrinn(trinnList);
                    }
                }
            });
        }
        return skoleOrganisasjon;
    }

}
