package no.fint.kulturtanken.model;

import lombok.Data;
import no.fint.kulturtanken.util.KulturtankenUtil;

import java.time.LocalDate;
import java.util.List;

@Data
public class Skoleeier {
	private String navn;
	private String organisasjonsnummer;
	private String skolear;
	private List<Skole> skoler;

	public static Skoleeier fromNsr(Enhet unit) {
		Skoleeier schoolOwner = new Skoleeier();
		schoolOwner.setNavn(unit.getNavn());
		schoolOwner.setOrganisasjonsnummer(unit.getOrgNr());
		schoolOwner.setSkolear(KulturtankenUtil.getSchoolYear(LocalDate.now()));
		return schoolOwner;
	}
}