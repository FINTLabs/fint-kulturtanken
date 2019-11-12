package no.fint.kulturtanken;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.Month;

@Slf4j
public final class KulturtankenUtil {

    private static LocalDate FALL_SEMESTER = LocalDate.of(LocalDate.now().getYear(), Month.AUGUST, 1);

    private KulturtankenUtil() {}

    public static String getSchoolYear(LocalDate nowDate) {
        if (nowDate.isBefore(FALL_SEMESTER)) {
            LocalDate pastDate = LocalDate.now().minusYears(1);
            return String.format("%d/%d", pastDate.getYear(), nowDate.getYear());
        } else {
            LocalDate futureDate = LocalDate.now().plusYears(1);
            return String.format("%d/%d", nowDate.getYear(), futureDate.getYear());
        }
    }
}
