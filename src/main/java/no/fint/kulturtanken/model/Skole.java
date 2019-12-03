package no.fint.kulturtanken.model;

import lombok.Data;

import java.util.List;

@Data
public class Skole {
	private String navn;
	private String organisasjonsnummer;
	private Kontaktinformasjon kontaktinformasjon;
	private Besoksadresse besoksadresse;
	private List<Trinn> trinn;
	private List<Fag> fag;
}