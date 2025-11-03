package no.fint.kulturtanken.model;

import lombok.Data;
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResource;

@Data
public class Undervisningsgruppe {
    private String navn;
    private int antall;

    public static Undervisningsgruppe fromFint(UndervisningsgruppeResource resource) {
        Undervisningsgruppe teachingGroup = new Undervisningsgruppe();
        teachingGroup.setNavn(resource.getNavn());
        teachingGroup.setAntall(resource.getGruppemedlemskap().size());
        return teachingGroup;
    }
}
