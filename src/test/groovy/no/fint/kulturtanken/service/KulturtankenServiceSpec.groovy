package no.fint.kulturtanken.service


import no.fint.kulturtanken.configuration.KulturtankenProperties
import no.fint.kulturtanken.model.Enhet
import no.fint.kulturtanken.model.Skole
import no.fint.kulturtanken.model.Skoleeier
import no.fint.kulturtanken.repository.FintRepository
import no.fint.kulturtanken.repository.NsrRepository
import no.fint.kulturtanken.util.FintObjectFactory
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class KulturtankenServiceSpec extends Specification {
    private KulturtankenService kulturtankenService
    private NsrRepository nsrRepository
    private FintRepository fintRepository
    private KulturtankenProperties kulturtankenProperties

    void setup() {
        nsrRepository = Mock()
        fintRepository = Mock()
        kulturtankenProperties = Mock()
        Clock clock = Clock.fixed(Instant.parse("2025-09-15T00:00:00Z"), ZoneId.systemDefault())
        kulturtankenService = new KulturtankenService(nsrRepository, fintRepository, kulturtankenProperties, clock)
    }

    def "Given valid orgId and source equal fint get school owner, schools and groups"() {
        given:
        def nsrSchool = new Enhet(navn: 'School', orgNr: '012345678',
                besoksadresse: new Enhet.Adresse(adress: 'Address', postnr: '0123', poststed: 'City'),
                epost: 'school@schools.no', telefon: '00 11 22 33', erAktiv: true,
                erVideregaaendeSkole: true, erOffentligSkole: true)
        def nsrSchoolOwner = new Enhet(navn: 'School owner', orgNr: '876543210',
                childRelasjoner: [new Enhet.ChildRelasjon(enhet: nsrSchool)])
        def school = FintObjectFactory.newSchool()
        def klasse = FintObjectFactory.newKlasse()
        def level = FintObjectFactory.newLevel()
        def teachingGroup = FintObjectFactory.newTeachingGroup()
        def subject = FintObjectFactory.newSubject()
        def skolear = FintObjectFactory.newSchoolYear()

        when:
        def schoolOwner = kulturtankenService.getSchoolOwner("876543210")

        then:
        1 * nsrRepository.getSchoolOwnerById(_ as String) >> Skoleeier.fromNsr(nsrSchoolOwner)
        1 * kulturtankenProperties.getOrganisations() >> ["876543210": new KulturtankenProperties.Organisation(source: 'fint')]
        1 * nsrRepository.getSchools(_ as String) >> [Skole.fromNsr(nsrSchool)]
        1 * fintRepository.getSchools(_ as String) >> [school]
        1 * fintRepository.getKlasseById(_ as String, _ as String) >> klasse
        1 * fintRepository.getLevelById(_ as String, _ as String) >> level
        1 * fintRepository.getTeachingGroupById(_ as String, _ as String) >> teachingGroup
        1 * fintRepository.getSubjectById(_ as String, _ as String) >> subject
        2 * fintRepository.getSkolearById(_ as String, _ as String) >> skolear

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
                besoksadresse: new Enhet.Adresse(adress: 'Address', postnr: '0123', poststed: 'City'),
                epost: 'school@schools.no', telefon: '00 11 22 33', erAktiv: true,
                erVideregaaendeSkole: true, erOffentligSkole: true)
        def nsrSchoolOwner = new Enhet(navn: 'School owner', orgNr: '876543210',
                childRelasjoner: [new Enhet.ChildRelasjon(enhet: nsrSchool)])

        when:
        def schoolOwner = kulturtankenService.getSchoolOwner(_ as String)

        then:
        1 * nsrRepository.getSchoolOwnerById(_ as String) >> Skoleeier.fromNsr(nsrSchoolOwner)
        1 * kulturtankenProperties.getOrganisations() >> [(_ as String): new KulturtankenProperties.Organisation(source: 'nsr')]
        1 * nsrRepository.getSchools(_ as String) >> [Skole.fromNsr(nsrSchool)]

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