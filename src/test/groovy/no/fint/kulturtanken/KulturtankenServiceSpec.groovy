package no.fint.kulturtanken


import no.fint.kulturtanken.model.Enhet
import no.fint.kulturtanken.service.FintService
import no.fint.kulturtanken.service.KulturtankenService
import no.fint.kulturtanken.service.NsrService
import spock.lang.Specification

class KulturtankenServiceSpec extends Specification {

    private KulturtankenService kulturtankenService
    private NsrService nsrService
    private FintService fintService
    private FintObjectFactory fintObjectFactory

    void setup() {
        nsrService = Mock()
        fintService = Mock()
        kulturtankenService = new KulturtankenService(nsrService, fintService)
        fintObjectFactory = new FintObjectFactory()
    }

    def "Get school owner"() {
        given:
        def nsrSchoolOwner = new Enhet(navn: 'Rogaland fylkeskommune', orgNr: '971045698')
        def nsrSchool = new Enhet(navn: 'Haugaland videregående skole', orgNr: '974624486',
                besoksadresse: new Enhet.Adresse(adresselinje: 'Spannavegen 38', postnunmmer: '5531', poststed: 'Haugesund' ))
        def school = fintObjectFactory.newSchoolResources()
        def basisGroup = fintObjectFactory.newBasisGroupResources()
        def level = fintObjectFactory.newLevelResources()
        def teachingGroup = fintObjectFactory.newTeachingGroupResources()
        def subject = fintObjectFactory.newSubjectResources()

        when:
        def response = kulturtankenService.getSchoolOwner('971045698')

        then:
        2 * nsrService.getUnit(_ as String) >>> [nsrSchoolOwner, nsrSchool]
        1 * fintService.getSchools(_ as String) >> school
        1 * fintService.getBasisGroups(_ as String) >> basisGroup
        1 * fintService.getLevels(_ as String) >> level
        1 * fintService.getTeachingGroups(_ as String) >> teachingGroup
        1 * fintService.getSubjects(_ as String) >> subject

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