package no.fint.kulturtanken.model

import spock.lang.Specification

class SkoleeierSpec extends Specification {

    def "Build school owner from NSR unit"() {
        given:
        def nsrSchoolOwner = new Enhet(navn: 'School owner', orgNr: '876543210')

        when:
        def schoolOwner = Skoleeier.fromNsr(nsrSchoolOwner)

        then:
        schoolOwner.navn == 'School owner'
        schoolOwner.organisasjonsnummer == '876543210'
    }
}
