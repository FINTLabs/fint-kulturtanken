package no.fint.kulturtanken.service

import no.fint.kulturtanken.configuration.KulturtankenProperties
import no.fint.kulturtanken.model.Enhet
import no.fint.kulturtanken.repository.FintRepository
import no.fint.kulturtanken.repository.NsrRepository
import no.fint.kulturtanken.util.FintObjectFactory
import no.fint.model.resource.Link
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources
import spock.lang.Specification

class KulturtankenServiceSpec extends Specification {

    private KulturtankenService kulturtankenService
    private NsrRepository nsrRepository
    private FintRepository fintRepository
    private KulturtankenProperties kulturtankenProperties

    void setup() {
        nsrRepository = Mock()
        fintRepository = Mock()
        kulturtankenProperties = Mock()
        kulturtankenService = new KulturtankenService(nsrRepository, fintRepository, kulturtankenProperties)
    }

    def "Given valid orgId and source equal fint get school owner, schools and groups"() {
        given:
        def nsrSchoolOwner = new Enhet(navn: 'School owner', orgNr: '876543210')
        def nsrSchool = new Enhet(navn: 'School', orgNr: '012345678',
                besoksadresse: new Enhet.Adresse(adress: 'Address', postnr: '0123', poststed: 'City' ))
        def school = FintObjectFactory.newSchool()
        def basisGroup = FintObjectFactory.newBasisGroup()
        def level = FintObjectFactory.newLevel()
        def teachingGroup = FintObjectFactory.newTeachingGroup()
        def subject = FintObjectFactory.newSubject()

        when:
        def schoolOwner = kulturtankenService.getSchoolOwner(_ as String)

        then:
        1 * nsrRepository.getUnit(_ as String) >> nsrSchoolOwner
        1 * kulturtankenProperties.getOrganisations() >> [(_ as String) : new KulturtankenProperties.Organisation(source: 'fint')]
        1 * nsrRepository.getUnit(_ as String) >> nsrSchool
        1 * fintRepository.getSchools(_ as String) >> [school]
        1 * fintRepository.getBasisGroups(_ as String) >> [(Link.with('link.To.BasisGroup')): basisGroup]
        1 * fintRepository.getLevels(_ as String) >> [(Link.with('link.To.Level')): level]
        1 * fintRepository.getTeachingGroups(_ as String) >> [(Link.with('link.To.TeachingGroup')): teachingGroup]
        1 * fintRepository.getSubjects(_ as String) >> [(Link.with('link.To.Subject')): subject]

        schoolOwner.navn == 'School owner'
        schoolOwner.organisasjonsnummer == '876543210'

        schoolOwner.skoler.size() == 1
        schoolOwner.skoler[0].navn == 'School'
        schoolOwner.skoler[0].organisasjonsnummer == '012345678'
        schoolOwner.skoler[0].kontaktinformasjon.epostadresse == 'school@schools.no'
        schoolOwner.skoler[0].kontaktinformasjon.telefonnummer == '00 11 22 33'
        schoolOwner.skoler[0].besoksadresse.adresselinje.any { it == 'Address' }
        schoolOwner.skoler[0].besoksadresse.postnummer == '0123'
        schoolOwner.skoler[0].besoksadresse.poststed == 'City'

        schoolOwner.skoler[0].trinn.size() == 1
        schoolOwner.skoler[0].trinn[0].niva == 'Grep'
        schoolOwner.skoler[0].trinn[0].basisgrupper.size() == 1
        schoolOwner.skoler[0].trinn[0].basisgrupper[0].navn == 'Basis group'
        schoolOwner.skoler[0].trinn[0].basisgrupper[0].antall == 1

        schoolOwner.skoler[0].fag.size() == 1
        schoolOwner.skoler[0].fag[0].fagkode == 'Grep'
        schoolOwner.skoler[0].fag[0].undervisningsgrupper.size() == 1
        schoolOwner.skoler[0].fag[0].undervisningsgrupper[0].navn == 'Teaching group'
        schoolOwner.skoler[0].fag[0].undervisningsgrupper[0].antall == 1
    }

    def "Given valid orgId and source equal nsr get school owner and schools"() {
        given:
        def nsrSchool = new Enhet(navn: 'School', orgNr: '012345678',
                besoksadresse: new Enhet.Adresse(adress: 'Address', postnr: '0123', poststed: 'City' ),
                epost: 'school@schools.no', telefon: '00 11 22 33', erAktiv: true,
                erVideregaaendeSkole: true, erOffentligSkole: true)
        def nsrSchoolOwner = new Enhet(navn: 'School owner', orgNr: '876543210',
                childRelasjoner: [new Enhet.ChildRelasjon(enhet: nsrSchool)])

        when:
        def schoolOwner = kulturtankenService.getSchoolOwner(_ as String)

        then:
        1 * nsrRepository.getUnit(_ as String) >> nsrSchoolOwner
        1 * kulturtankenProperties.getOrganisations() >> [(_ as String) : new KulturtankenProperties.Organisation(source: 'nsr')]
        1 * nsrRepository.getUnit(_ as String) >> nsrSchool

        schoolOwner.navn == 'School owner'
        schoolOwner.organisasjonsnummer == '876543210'

        schoolOwner.skoler.size() == 1
        schoolOwner.skoler[0].navn == 'School'
        schoolOwner.skoler[0].organisasjonsnummer == '012345678'
        schoolOwner.skoler[0].kontaktinformasjon.epostadresse == 'school@schools.no'
        schoolOwner.skoler[0].kontaktinformasjon.telefonnummer == '00 11 22 33'
        schoolOwner.skoler[0].besoksadresse.adresselinje.any { it == 'Address' }
        schoolOwner.skoler[0].besoksadresse.postnummer == '0123'
        schoolOwner.skoler[0].besoksadresse.poststed == 'City'

        schoolOwner.skoler[0].trinn.size() == 0
        schoolOwner.skoler[0].fag.size() == 0
    }
}