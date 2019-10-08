package no.fint.kulturtanken

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.test.utils.MockMvcSpecification
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

class KulturtankenServiceSpec extends MockMvcSpecification {

    private MockWebServer server
    private KulturtankenService kulturtankenService
    private FintObjectFactory fintObjectFactory
    private ObjectMapper objectMapper

    void setup() {
        server = new MockWebServer()
        kulturtankenService = new KulturtankenService(RestTemplateBuilder.newInstance().rootUri(server.url('/').toString()).build())
        fintObjectFactory = new FintObjectFactory()
        objectMapper = new ObjectMapper()
    }

    def "Get school owner"() {
        given:
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(objectMapper.writeValueAsString(fintObjectFactory.newOrganizationElementResources())))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(objectMapper.writeValueAsString(fintObjectFactory.newSchoolResources())))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(objectMapper.writeValueAsString(fintObjectFactory.newBasisGroupResources())))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(objectMapper.writeValueAsString(fintObjectFactory.newLevelResources())))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(objectMapper.writeValueAsString(fintObjectFactory.newTeachingGroupResources())))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(objectMapper.writeValueAsString(fintObjectFactory.newSubjectResources())))

        when:
        def response = kulturtankenService.getSchoolOwner('bearer')

        then:
        response.navn == 'Rogaland fylkeskommune'
        response.organisasjonsnummer == '971045698'
        response.kontaktinformasjon.epostadresse == 'firmapost@rogfk.no'
        response.kontaktinformasjon.telefonnummer == '51 51 66 00'

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