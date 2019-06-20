package no.fint.kulturtanken.model;

import lombok.Data;

import java.util.List;

@Data
public class Skole {
	private Kontaktinformasjon kontaktinformasjon;
	private List<Trinn> trinn;
	private String navn;
	private String organisasjonsnummer;
	private String skolenummer;
}