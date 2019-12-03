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

    def "Given valid orgId get children of Unit from NSR"() {
        given:
        def nsrChildUnit = new Enhet(navn: 'Haugaland videregående skole', orgNr: '974624486',
                besoksadresse: new Enhet.Adresse(adress: 'Spannavegen 38', postnr: '5531', poststed: 'Haugesund'),
                erOffentligSkole: true, erVideregaaendeSkole: true, erAktiv: true)
        def nsrUnit = new Enhet(navn: 'Rogaland fylkeskommune', orgNr: '971045698',
                besoksadresse: new Enhet.Adresse(adress: 'Arkitekt Eckhoffs Gate 1', postnr: '4010', poststed: 'STAVANGER'),
                childRelasjoner: [new Enhet.ChildRelasjon(enhet: nsrChildUnit)])

        when:
        def fintUnits = nsrService.getUnits(_ as String)

        then:
        2 * restTemplate.getForObject(_, _ as Class<Enhet>, _ as String) >>> [nsrUnit, nsrChildUnit]
        fintUnits.size() == 1
        fintUnits.get(0).navn == 'Haugaland videregående skole'
    }
}