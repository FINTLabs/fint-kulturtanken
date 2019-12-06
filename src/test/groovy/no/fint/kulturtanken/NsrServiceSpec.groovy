package no.fint.kulturtanken

import no.fint.kulturtanken.model.Enhet
import no.fint.kulturtanken.service.NsrService
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class NsrServiceSpec extends Specification {
    private NsrService nsrService
    private RestTemplate restTemplate

    void setup() {
        restTemplate = Mock()
        nsrService = new NsrService(restTemplate)
    }

    def "Given valid orgId get Unit from NSR"() {
        given:
        def nsrUnit = new Enhet(navn: 'Rogaland fylkeskommune', orgNr: '971045698',
                besoksadresse: new Enhet.Adresse(adress: 'Arkitekt Eckhoffs Gate 1', postnr: '4010', poststed: 'STAVANGER'))
        when:
        def fintUnit = nsrService.getUnit(_ as String)

        then:
        1 * restTemplate.getForObject(_, _ as Class<Enhet>, _ as String) >> nsrUnit
        fintUnit.navn == 'Rogaland fylkeskommune'
    }
}