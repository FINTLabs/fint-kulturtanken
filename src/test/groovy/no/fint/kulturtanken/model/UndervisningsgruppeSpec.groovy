package no.fint.kulturtanken.model

import no.fint.kulturtanken.util.FintObjectFactory
import spock.lang.Specification

class UndervisningsgruppeSpec extends Specification {

    def "Build teaching group from Fint teaching group"() {
        given:
        def fintTeachingGroup = FintObjectFactory.newTeachingGroup()

        when:
        def teachingGroup = Undervisningsgruppe.fromFint(fintTeachingGroup)

        then:
        teachingGroup.navn == 'Teaching group'
        teachingGroup.antall == 1
    }
}
