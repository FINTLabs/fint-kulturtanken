package no.fint.kulturtanken.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Enhet {

    @JsonProperty("Navn")
    private String navn;

    @JsonProperty("OrgNr")
    private String orgNr;

    @JsonProperty("ErOffentligSkole")
    private Boolean erOffentligSkole;

    @JsonProperty("ErVideregaaendeSkole")
    private Boolean erVideregaaendeSkole;

    @JsonProperty("ErAktiv")
    private Boolean erAktiv;

    @JsonProperty("Epost")
    private String epost;

    @JsonProperty("Telefon")
    private String telefon;

    @JsonProperty("Besoksadresse")
    private Adresse besoksadresse;

    @JsonProperty("ChildRelasjoner")
    private List<ChildRelasjon> childRelasjoner;

    @Data
    public static class Adresse {

        @JsonProperty("Adress")
        private String adress;

        @JsonProperty("Postnr")
        private String postnr;

        @JsonProperty("Poststed")
        private String poststed;
    }

    @Data
    public static class ChildRelasjon {

        @JsonProperty("Enhet")
        private Enhet enhet;
    }
}
