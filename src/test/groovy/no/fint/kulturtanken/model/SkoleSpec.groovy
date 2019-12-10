package no.fint.kulturtanken.model

import no.fint.kulturtanken.util.FintObjectFactory
import spock.lang.Specification

class SkoleSpec extends Specification {

    def "Build school from Fint school"() {
        given:
        def fintSchool = FintObjectFactory.newSchool()

        when:
        def school = Skole.fromFint(fintSchool)

        then:
        school.navn == 'School'
        school.organisasjonsnummer == '012345678'
        school.kontaktinformasjon.epostadresse == 'school@schools.no'
        school.kontaktinformasjon.telefonnummer == '00 11 22 33'
    }

    def "Build school from NSR unit"() {
        given:
        def nsrSchool = new Enhet(navn: 'School', orgNr: '012345678',
                besoksadresse: new Enhet.Adresse(adress: 'Address', postnr: '0123', poststed: 'City' ),
                epost: 'school@schools.no', telefon: '00 11 22 33')

        when:
        def school = Skole.fromNsr(nsrSchool)

        then:
        school.navn == 'School'
        school.organisasjonsnummer == '012345678'
        school.kontaktinformasjon.epostadresse == 'school@schools.no'
        school.kontaktinformasjon.telefonnummer == '00 11 22 33'
    }
}
