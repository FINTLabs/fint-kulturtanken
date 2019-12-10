package no.fint.kulturtanken.model

import no.fint.kulturtanken.util.FintObjectFactory
import spock.lang.Specification

class BasisGruppeSpec extends Specification {

    def "Build basis group from Fint basis group"() {
        given:
        def fintBasisGroup = FintObjectFactory.newBasisGroup()

        when:
        def basisGroup = Basisgruppe.fromFint(fintBasisGroup)

        then:
        basisGroup.navn == 'Basis group'
        basisGroup.antall == 1
    }
}
