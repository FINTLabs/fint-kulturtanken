package no.fint.kulturtanken.model;

import lombok.Data;

import java.util.List;

@Data
public class SkoleEier {
	private String navn;
	private String organisasjonsnummer;
	private List<Skole> skole;
	private Kontaktinformasjon kontaktinformasjon;

}