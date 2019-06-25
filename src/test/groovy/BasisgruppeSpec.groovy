import no.fint.model.resource.Link
import no.fint.model.resource.utdanning.elev.BasisgruppeResource
import spock.lang.Specification

class BasisgruppeSpec extends Specification {
    def "Tester litt"() {
        given:
        def basisgruppe = new BasisgruppeResource(
                navn: 'Hei hei',
                beskrivelse: 'hallo'
        )
        basisgruppe.addSkole(Link.with('http://bo.skule.no'))

        when:
        println(basisgruppe)

        then:
        noExceptionThrown()
        basisgruppe.links.any { it.key == 'skule'}
    }
}