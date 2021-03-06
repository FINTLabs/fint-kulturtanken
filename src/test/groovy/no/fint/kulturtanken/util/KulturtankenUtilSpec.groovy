package no.fint.kulturtanken.util

import spock.lang.Specification

import java.time.LocalDate

class KulturtankenUtilSpec extends Specification {

    def "Given date in fall return school year"() {
        given:
        def date = LocalDate.parse('2019-08-01')

        when:
        def schoolYear = KulturtankenUtil.getSchoolYear(date)

        then:
        schoolYear == '2019/2020'
    }

    def "Given date in spring return school year"() {
        given:
        def date = LocalDate.parse('2019-07-31')

        when:
        def schoolYear = KulturtankenUtil.getSchoolYear(date)

        then:
        schoolYear == '2018/2019'
    }
}