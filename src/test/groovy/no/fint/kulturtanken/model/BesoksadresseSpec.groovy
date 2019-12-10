package no.fint.kulturtanken.model

import no.fint.kulturtanken.util.FintObjectFactory
import spock.lang.Specification

class BesoksadresseSpec extends Specification {

    def "Build visiting address from Fint business address"() {
        given:
        def school = FintObjectFactory.newSchool()

        when:
        def visitingAddress = Besoksadresse.fromFint(school.getForretningsadresse())

        then:
        visitingAddress.adresselinje == ['Address']
        visitingAddress.postnummer == '0123'
        visitingAddress.poststed == 'City'
    }

    def "build visiting address from NSR visiting address"() {
        given:
        def school = new Enhet(navn: 'School', orgNr: '012345678',
                besoksadresse: new Enhet.Adresse(adress: 'Address', postnr: '0123', poststed: 'City'))

        when:
        def visitingAddress = Besoksadresse.fromNsr(school.getBesoksadresse())

        then:
        visitingAddress.adresselinje == ['Address']
        visitingAddress.postnummer == '0123'
        visitingAddress.poststed == 'City'
    }
}
