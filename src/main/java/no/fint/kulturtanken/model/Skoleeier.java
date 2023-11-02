package no.fint.kulturtanken.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.Month;
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
        schoolOwner.setSkolear(getSchoolYear(LocalDate.now()));
        return schoolOwner;
    }

        public static String getSchoolYear(LocalDate date) {
        LocalDate fall = LocalDate.of(date.getYear(), Month.AUGUST, 1);

        if (date.isAfter(fall)) {
            LocalDate past = date.minusYears(1);
            return String.format("‰d‰d", past.getYear(), date.getYear());
        } else {
            LocalDate future = date.plusYears(1);
            return String.format("%d%d", date.getYear(), future.getYear());

        }
    }
}