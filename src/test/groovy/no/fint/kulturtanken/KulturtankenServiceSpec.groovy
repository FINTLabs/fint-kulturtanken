package no.fint.kulturtanken

import no.fint.kulturtanken.config.KulturtankenProperties
import no.fint.kulturtanken.model.Enhet
import no.fint.kulturtanken.service.FintService
import no.fint.kulturtanken.service.KulturtankenService
import no.fint.kulturtanken.service.NsrService
import no.fint.model.resource.Link
import spock.lang.Specification

class KulturtankenServiceSpec extends Specification {

    private KulturtankenService kulturtankenService
    private NsrService nsrService
    private FintService fintService
    private KulturtankenProperties kulturtankenProperties
    private FintObjectFactory fintObjectFactory

    void setup() {
        nsrService = Mock()
        fintService = Mock()
        kulturtankenProperties = Mock()
        kulturtankenService = new KulturtankenService(nsrService, fintService, kulturtankenProperties)
        fintObjectFactory = new FintObjectFactory()
    }

    def "Given valid orgId and source equal fint get school owner, schools and groups"() {
        given:
        def nsrSchoolOwner = new Enhet(navn: 'Rogaland fylkeskommune', orgNr: '971045698')
        def nsrSchool = new Enhet(navn: 'Haugaland videregående skole', orgNr: '974624486',
                besoksadresse: new Enhet.Adresse(adress: 'Spannavegen 38', postnr: '5531', poststed: 'Haugesund' ))
        def schools = fintObjectFactory.newSchoolResources()
        def basisGroup = fintObjectFactory.newBasisGroup()
        def level = fintObjectFactory.newLevel()
        def teachingGroup = fintObjectFactory.newTeachingGroup()
        def subject = fintObjectFactory.newSubject()

        when:
        def response = kulturtankenService.getSchoolOwner(_ as String)

        then:
        1 * nsrService.getUnit(_ as String) >> nsrSchoolOwner
        1 * kulturtankenProperties.getOrganisations() >> [(_ as String) : new KulturtankenProperties.Organisation(source: 'fint')]
        1 * nsrService.getUnit(_ as String) >> nsrSchool
        1 * fintService.getSchools(_ as String) >> schools
        1 * fintService.getBasisGroups(_ as String) >> [(new Link(verdi: 'https://hvs')): [(new Link(verdi: 'https://vg1')): [basisGroup]]]
        1 * fintService.getLevels(_ as String) >> [(new Link(verdi: 'https://vg1')): level]
        1 * fintService.getTeachingGroups(_ as String) >> [(new Link(verdi: 'https://hvs')): [(new Link(verdi: 'https://yff4106')): [teachingGroup]]]
        1 * fintService.getSubjects(_ as String) >> [(new Link('https://yff4106')): subject]

        response.navn == 'Rogaland fylkeskommune'
        response.organisasjonsnummer == '971045698'

        response.skoler.size() == 1
        response.skoler[0].navn == 'Haugaland videregående skole'
        response.skoler[0].organisasjonsnummer == '974624486'
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

    def "Given valid orgId and source equal nsr get school owner and schools"() {
        given:
        def nsrSchoolOwner = new Enhet(navn: 'Rogaland fylkeskommune', orgNr: '971045698')
        def nsrSchool = new Enhet(navn: 'Haugaland videregående skole', orgNr: '974624486',
                besoksadresse: new Enhet.Adresse(adress: 'Spannavegen 38', postnr: '5531', poststed: 'Haugesund' ),
                epost: 'haugaland-vgs@skole.rogfk.no', telefon: '52 86 56 00')

        when:
        def response = kulturtankenService.getSchoolOwner(_ as String)

        then:
        1 * nsrService.getUnit(_ as String) >> nsrSchoolOwner
        1 * kulturtankenProperties.getOrganisations() >> [(_ as String) : new KulturtankenProperties.Organisation(source: 'nsr')]
        1 * nsrService.getUnits(_ as String) >> [nsrSchool]

        response.navn == 'Rogaland fylkeskommune'
        response.organisasjonsnummer == '971045698'

        response.skoler.size() == 1
        response.skoler[0].navn == 'Haugaland videregående skole'
        response.skoler[0].organisasjonsnummer == '974624486'
        response.skoler[0].kontaktinformasjon.epostadresse == 'haugaland-vgs@skole.rogfk.no'
        response.skoler[0].kontaktinformasjon.telefonnummer == '52 86 56 00'
        response.skoler[0].besoksadresse.adresselinje.any { it == 'Spannavegen 38' }
        response.skoler[0].besoksadresse.postnummer == '5531'
        response.skoler[0].besoksadresse.poststed == 'Haugesund'

        response.skoler[0].trinn.size() == 0
        response.skoler[0].fag.size() == 0
    }
}