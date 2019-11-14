package no.fint.kulturtanken

import no.fint.kulturtanken.model.Enhet
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class NsrServiceSpec extends Specification {
    private NsrService nsrService
    private RestTemplate restTemplate

    void setup() {
        restTemplate = Mock()
        nsrService = new NsrService(restTemplate)
    }

    def "Given orgId get Unit from NSR"() {
        given:
        def unit = new Enhet(besoksadresse: new Enhet.Adresse(adresselinje: 'Salhusvegen 68', postnunmmer: '5529', poststed: '5529' ))

        when:
        def visitingAddress = nsrService.getVisitingAddress(_ as String)

        then:
        1 * restTemplate.getForObject(_, _ as Class<Enhet>, _ as String) >> unit
        visitingAddress.adresselinje.get(0) == 'Salhusvegen 68'
        visitingAddress.postnummer == '5529'
        visitingAddress.poststed == '5529'
    }
}