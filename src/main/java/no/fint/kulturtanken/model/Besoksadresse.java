package no.fint.kulturtanken.model;

import lombok.Data;
import no.fint.model.resource.felles.kompleksedatatyper.AdresseResource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Data
public class Besoksadresse {
    private List<String> adresselinje;
    private String postnummer;
    private String poststed;

    public static Besoksadresse fromNsr(Enhet.Adresse address) {
        Besoksadresse visitingAddress = new Besoksadresse();
        visitingAddress.setAdresselinje(Collections.singletonList(address.getAdress()));
        visitingAddress.setPostnummer(address.getPostnr());
        visitingAddress.setPoststed(address.getPoststed());
        return visitingAddress;
    }

    public static Besoksadresse fromFint(AdresseResource resource) {
        Besoksadresse visitingAddress = new Besoksadresse();
        visitingAddress.setAdresselinje(resource.getAdresselinje());
        visitingAddress.setPostnummer(resource.getPostnummer());
        visitingAddress.setPoststed(resource.getPoststed());
        return visitingAddress;
    }
}
