package no.fint.kulturtanken.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Enhet {

    @JsonProperty(value = "Navn")
    private String navn;

    @JsonProperty(value = "OrgNr")
    private String orgNr;

    @JsonProperty(value = "Besoksadresse")
    private Adresse besoksadresse;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Adresse {
        @JsonProperty(value = "Adress")
        private String adresselinje;

        @JsonProperty(value = "Postnr")
        private String postnunmmer;

        @JsonProperty(value = "Poststed")
        private String poststed;
    }
}
