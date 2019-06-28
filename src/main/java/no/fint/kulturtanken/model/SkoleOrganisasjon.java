package no.fint.kulturtanken.model;

import lombok.Data;

import java.util.List;

@Data
public class SkoleOrganisasjon {
	private String navn;
	private String organisasjonsnummer;
	private List<Skole> skole;
	private Kontaktinformasjon kontaktinformasjon;

}