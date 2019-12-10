package no.fint.kulturtanken.repository

import no.fint.kulturtanken.model.Enhet
import no.fint.kulturtanken.repository.NsrRepository
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class NsrRepositorySpec extends Specification {
    private NsrRepository nsrRepository
    private RestTemplate restTemplate

    void setup() {
        restTemplate = Mock()
        nsrRepository = new NsrRepository(restTemplate)
    }

    def "Given valid orgId get Unit from NSR"() {
        given:
        def nsrUnit = new Enhet(navn: 'School owner', orgNr: '876543210',
                besoksadresse: new Enhet.Adresse(adress: 'Address', postnr: '0123', poststed: 'City'))

        when:
        def fintUnit = nsrRepository.getUnit(_ as String)

        then:
        1 * restTemplate.getForObject(_, _ as Class<Enhet>, _ as String) >> nsrUnit
        fintUnit.navn == 'School owner'
    }
}