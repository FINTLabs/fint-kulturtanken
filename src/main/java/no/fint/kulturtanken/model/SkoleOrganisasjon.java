package no.fint.kulturtanken.model;

import lombok.Data;

import java.util.List;

@Data
public class SkoleOrganisasjon {
	private String navn;
	private String organisasjonsnummer;
	private Kontaktinformasjon kontaktinformasjon;
	private List<Skole> skole;
}