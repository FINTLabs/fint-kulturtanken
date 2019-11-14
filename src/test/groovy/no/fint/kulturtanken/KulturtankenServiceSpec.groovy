package no.fint.kulturtanken

import no.fint.kulturtanken.model.Besoksadresse
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class KulturtankenServiceSpec extends Specification {

    private KulturtankenService kulturtankenService
    private NsrService nsrService
    private FintObjectFactory fintObjectFactory
    private RestTemplate restTemplate

    void setup() {
        nsrService = Mock()
        restTemplate = Mock()
        kulturtankenService = new KulturtankenService(restTemplate, nsrService)
        fintObjectFactory = new FintObjectFactory()
    }

    def "Get school owner"() {
        given:
        def visitingAddress = new Besoksadresse(adresselinje: ['Spannavegen 38'], postnummer: '5531', poststed: 'Haugesund')
        def organisationElement = fintObjectFactory.newOrganizationElementResources()
        def school = fintObjectFactory.newSchoolResources()
        def basisGroup = fintObjectFactory.newBasisGroupResources()
        def level = fintObjectFactory.newLevelResources()
        def teachingGroup = fintObjectFactory.newTeachingGroupResources()
        def subject = fintObjectFactory.newSubjectResources()

        when:
        def response = kulturtankenService.getSchoolOwner()

        then:
        6 * restTemplate.getForObject(_, _ as Class) >>> [organisationElement, school, basisGroup, level, teachingGroup, subject]
        1 * nsrService.getVisitingAddress('974624486') >> visitingAddress

        response.navn == 'Rogaland fylkeskommune'
        response.organisasjonsnummer == '971045698'

        response.skoler.size() == 1
        response.skoler[0].navn == 'Haugaland videregående skole'
        response.skoler[0].organisasjonsnummer == '974624486'
        response.skoler[0].skolenummer == '11011'
        response.skoler[0].kontaktinformasjon.epostadresse == 'haugaland-vgs@skole.rogfk.no'
        response.skoler[0].kontaktinformasjon.telefonnummer == '52 86 56 00'
        response.skoler[0].besoksadresse.adresselinje.any { it == 'Spannavegen 38' }
        response.skoler[0].besoksadresse.postnummer == '5531'
        response.skoler[0].besoksadresse.poststed == 'Haugesund'

        response.skoler[0].trinn.size() == 1
        response.skoler[0].trinn[0].niva == 'vg1'
        response.skoler[0].trinn[0].basisgrupper.size() == 1
        response.skoler[0].trinn[0].basisgrupper[0].navn == '1TIA_HVS'
        response.skoler[0].trinn[0].basisgrupper[0].antall == 1

        response.skoler[0].fag.size() == 1
        response.skoler[0].fag[0].fagkode == 'YFF4106'
        response.skoler[0].fag[0].undervisningsgrupper.size() == 1
        response.skoler[0].fag[0].undervisningsgrupper[0].navn == 'YFF4106_HVS'
        response.skoler[0].fag[0].undervisningsgrupper[0].antall == 1
    }
}