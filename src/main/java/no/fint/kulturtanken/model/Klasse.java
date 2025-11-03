package no.fint.kulturtanken.model;

import lombok.Data;
import no.fint.model.resource.utdanning.elev.KlasseResource;

@Data
public class Klasse {
    private String navn;
    private int antall;

    public static Klasse fromFint(KlasseResource resource) {
        Klasse klasse = new Klasse();
        klasse.setNavn(resource.getNavn());
        klasse.setAntall(resource.getKlassemedlemskap().size());
        return klasse;
    }
}
