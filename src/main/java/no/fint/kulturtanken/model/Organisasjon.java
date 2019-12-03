package no.fint.kulturtanken.model;

import lombok.Data;

import java.net.URI;

@Data
public class Organisasjon {
    private String navn;
    private String organisasjonsnummer;
    private Boolean grupper;
    private URI uri;
}
