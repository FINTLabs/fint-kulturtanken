package no.fint.kulturtanken.model;

import lombok.Data;
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResource;

import java.util.Collections;
import java.util.List;

@Data
public class Skole {
	private String navn;
	private String organisasjonsnummer;
	private Kontaktinformasjon kontaktinformasjon;
	private Besoksadresse besoksadresse;
	private List<Trinn> trinn;
	private List<Fag> fag;

	public static Skole fromNsr(Enhet unit) {
		Skole school = new Skole();
		school.setNavn(unit.getNavn());
		school.setOrganisasjonsnummer(unit.getOrgNr());

		Kontaktinformasjon contactInformation = new Kontaktinformasjon();
		contactInformation.setEpostadresse(unit.getEpost());
		contactInformation.setTelefonnummer(unit.getTelefon());
		school.setKontaktinformasjon(contactInformation);

		Besoksadresse visitingAddress = new Besoksadresse();
		if (unit.getBesoksadresse() != null) {
			visitingAddress.setAdresselinje(Collections.singletonList(unit.getBesoksadresse().getAdress()));
			visitingAddress.setPostnummer(unit.getBesoksadresse().getPostnr());
			visitingAddress.setPoststed(unit.getBesoksadresse().getPoststed());
		}
		school.setBesoksadresse(visitingAddress);

		school.setTrinn(Collections.emptyList());
		school.setFag(Collections.emptyList());
		return school;
	}

	public static Skole fromFint(SkoleResource resource) {
		Skole school = new Skole();
		school.setNavn(resource.getNavn());

		if (resource.getOrganisasjonsnummer() != null) {
			school.setOrganisasjonsnummer(resource.getOrganisasjonsnummer().getIdentifikatorverdi());
		}

		Kontaktinformasjon contactInformation = new Kontaktinformasjon();
		if (resource.getKontaktinformasjon() != null) {
			contactInformation.setEpostadresse(resource.getKontaktinformasjon().getEpostadresse());
			contactInformation.setTelefonnummer(resource.getKontaktinformasjon().getTelefonnummer());
		}
		school.setKontaktinformasjon(contactInformation);

		return school;
	}
}