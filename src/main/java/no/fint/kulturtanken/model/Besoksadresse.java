package no.fint.kulturtanken.model;

import lombok.Data;

import java.util.List;

@Data
public class Besoksadresse {
    private List<String> adresselinje;
    private String postnummer;
    private String poststed;
}
