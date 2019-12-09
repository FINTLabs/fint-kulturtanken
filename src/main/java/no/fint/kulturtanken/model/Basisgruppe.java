package no.fint.kulturtanken.model;

import lombok.Data;
import no.fint.model.resource.utdanning.elev.BasisgruppeResource;

@Data
public class Basisgruppe {
    private String navn;
    private int antall;

    public static Basisgruppe fromFint(BasisgruppeResource resource) {
        Basisgruppe basisGroup = new Basisgruppe();
        basisGroup.setNavn(resource.getNavn());
        basisGroup.setAntall(resource.getElevforhold().size());
        return basisGroup;
    }
}
