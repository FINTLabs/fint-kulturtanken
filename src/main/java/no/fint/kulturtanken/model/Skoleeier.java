package no.fint.kulturtanken.model;

import lombok.Data;

import java.util.List;

@Data
public class Skoleeier {
	private String navn;
	private String organisasjonsnummer;
	private String skolear;
	private List<Skole> skoler;
}