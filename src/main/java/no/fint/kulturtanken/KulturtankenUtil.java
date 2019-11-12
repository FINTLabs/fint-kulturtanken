package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.Month;

import static java.time.LocalDate.now;

public final class KulturtankenUtil {

    private KulturtankenUtil() {}

    public static String getSchoolYear(LocalDate date) {
        LocalDate fall = LocalDate.of(date.getYear(), Month.AUGUST, 1);

        if (date.isBefore(fall)) {
            LocalDate past = date.minusYears(1);
            return String.format("%d/%d", past.getYear(), date.getYear());
        } else {
            LocalDate future = date.plusYears(1);
            return String.format("%d/%d", date.getYear(), future.getYear());
        }
    }
}
