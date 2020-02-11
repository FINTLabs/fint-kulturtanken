package no.fint.kulturtanken.controller

import groovy.json.JsonOutput
import no.fint.kulturtanken.configuration.KulturtankenProperties
import no.fint.kulturtanken.model.Basisgruppe
import no.fint.kulturtanken.model.Besoksadresse
import no.fint.kulturtanken.model.Fag
import no.fint.kulturtanken.model.Kontaktinformasjon

import no.fint.kulturtanken.model.Skole
import no.fint.kulturtanken.model.Skoleeier
import no.fint.kulturtanken.model.Trinn
import no.fint.kulturtanken.model.Undervisningsgruppe
import no.fint.kulturtanken.service.KulturtankenService

import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

class KulturtankenControllerSpec extends Specification {
    KulturtankenService kulturtankenService
    KulturtankenProperties kulturtankenProperties
    KulturtankenController kulturtankenController
    MockMvc mockMvc

    void setup() {
        kulturtankenService = Mock()
        kulturtankenProperties = Mock()
        kulturtankenController = new KulturtankenController(kulturtankenService, kulturtankenProperties)
        mockMvc = MockMvcBuilders.standaloneSetup(kulturtankenController).build()
    }

    def "Given valid orgId return school owner"() {
        given:
        def schoolOwner = new Skoleeier(navn: 'School owner', organisasjonsnummer: '876543210',
                skolear: '2019/2020', skoler: [new Skole(navn: 'School', organisasjonsnummer: '012345678', kontaktinformasjon:
                new Kontaktinformasjon(epostadresse: 'school@schools.no', telefonnummer: '00 11 22 33'), besoksadresse:
                new Besoksadresse(adresselinje: ['Address'], postnummer: '0123', poststed: 'City'), trinn:
                [new Trinn(niva: 'Level', basisgrupper: [new Basisgruppe(navn: 'Basis group', antall: 1)])], fag:
                [new Fag(fagkode: 'Subject', undervisningsgrupper: [new Undervisningsgruppe(navn: 'Teaching group', antall: 1)])])])

        when:
        def response = mockMvc.perform(get("/skoleeier/876543210"))

        then:
        1 * kulturtankenProperties.getOrganisations() >> [('876543210'): new KulturtankenProperties.Organisation()]
        1 * kulturtankenService.getSchoolOwner('876543210') >> schoolOwner
        response.andExpect(status().isOk()).andExpect(content().json(JsonOutput.toJson(schoolOwner)))
    }

    def "Given invalid orgId returns not found"() {
        when:
        def response = mockMvc.perform(get("/skoleeier/876543210"))

        then:
        1 * kulturtankenProperties.getOrganisations() >> Collections.emptyMap()
        response.andExpect(status().isNotFound())
    }

    def "Get all organisations"() {
        when:
        def response = mockMvc.perform(get("/skoleeier/"))

        then:
        1 * kulturtankenProperties.getOrganisations() >> [(_ as String): new KulturtankenProperties.Organisation()]
        response.andExpect(status().isOk())
    }

}
