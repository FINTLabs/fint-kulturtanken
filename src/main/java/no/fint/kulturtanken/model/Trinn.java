package no.fint.kulturtanken.model;

import lombok.Data;

import java.util.List;

@Data
public class Trinn {
	private String niva;
	private List<Klasse> basisgrupper;
}