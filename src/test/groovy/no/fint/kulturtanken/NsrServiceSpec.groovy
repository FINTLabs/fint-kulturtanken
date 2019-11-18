package no.fint.kulturtanken

import no.fint.kulturtanken.model.Enhet
import no.fint.kulturtanken.service.NsrService
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
        def unit = new Enhet(navn: 'Haugaland videregående skole', orgNr: '974624486',
                besoksadresse: new Enhet.Adresse(adresselinje: 'Spannavegen 38', postnunmmer: '5531', poststed: 'Haugesund' ))

        when:
        nsrService.getUnit(_ as String)

        then:
        1 * restTemplate.getForObject(_, _ as Class<Enhet>, _ as String) >> unit
    }
}