package no.fint.kulturtanken.model

import no.fint.kulturtanken.util.FintObjectFactory
import spock.lang.Specification

class KlasseSpec extends Specification {

    def "Build klasse from Fint klasse"() {
        given:
        def fintKlasse = FintObjectFactory.newKlasse()

        when:
        def klasse = Klasse.fromFint(fintKlasse)

        then:
        klasse.navn == 'Klasse'
        klasse.antall == 1
    }
}
