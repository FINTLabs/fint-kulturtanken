package no.fint.kulturtanken.model;

import lombok.Data;

import java.util.List;

@Data
public class Skoleeier{
	private Kontaktinformasjon kontaktinformasjon;
	private List<Skole> skole;
	private String navn;
	private String organisasjonsnummer;
}