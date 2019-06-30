package no.fint.kulturtanken

import no.fint.kulturtanken.model.SkoleOrganisasjon
import no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResources
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

class FintServiceSpec extends Specification {
    private def server = new MockWebServer()
    private def webClient = WebClient.create(server.url('/').toString())
    private def fintserviceTestComponents = new FintService(webClient: webClient)


    def "Use addOrganisationInfo() and receive Name, Kontaktinformasjon and emtpty school"() {
        given:
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("skoleOrganisasjonResources.json").getFile().text))
        when:
        SkoleOrganisasjon skoleOrganisasjon = new SkoleOrganisasjon()
        fintserviceTestComponents.addOrganisationInfo(skoleOrganisasjon, "testBearer")

        then:
        skoleOrganisasjon.navn == "Haugaland fylkeskommune"
        skoleOrganisasjon.kontaktinformasjon.mobiltelefonnummer == "47474747"
        skoleOrganisasjon.kontaktinformasjon.epostadresse == "post@haugfk.no"
        skoleOrganisasjon.skole == null
    }

    def "Extract information by getKontaktInformation and receive mail, and number"() {
        given:
        Kontaktinformasjon kontaktinformasjon = new Kontaktinformasjon()
        kontaktinformasjon.setEpostadresse("test-email@testing.test")
        kontaktinformasjon.setMobiltelefonnummer("99999999")
        when:
        def returnedKontaktInformasjon = fintserviceTestComponents.getKontaktInformasjon(kontaktinformasjon)

        then:
        returnedKontaktInformasjon.epostadresse == "test-email@testing.test"
        returnedKontaktInformasjon.mobiltelefonnummer == "99999999"
    }

    def "Use getSkoleList to receive List with Skole"() {
        given:
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("skoleResources.json").getFile().text))
        when:
        def schoolList = fintserviceTestComponents.getSkoleList("testBearer")

        then:
        schoolList.size() == 2
        schoolList[0].navn == "Bø VGS"
        schoolList[0].trinn == null
        schoolList[0].organisasjonsnummer == "98765432"
        schoolList[0].skolenummer == "12345"
        schoolList[1].navn == "Sundet VGS"
        schoolList[1].trinn == null
        schoolList[1].organisasjonsnummer == "970123458"
        schoolList[1].skolenummer == "123456"
    }

    def "Add SkoleOrganisasjon, Skole, Trinn and Basisgrupper. Then test Trinn-structure"() {
        given:
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("skoleOrganisasjonResources.json").getFile().text))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("skoleResources.json").getFile().text))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("skoleResources.json").getFile().text))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("aarstrinnResources.json").getFile().text))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("basisgrupperResources.json").getFile().text))
        when:
        SkoleOrganisasjon skoleOrganisasjon = new SkoleOrganisasjon()
        fintserviceTestComponents.addOrganisationInfo(skoleOrganisasjon, "testBearer")
        skoleOrganisasjon.skole = fintserviceTestComponents.getSkoleList("testBearer")
        fintserviceTestComponents.setSchoolLevelsAndGroups(skoleOrganisasjon, "testBearer")

        then:
        skoleOrganisasjon.skole[0].trinn.size() == 2
        skoleOrganisasjon.skole[0].trinn[0].niva == "1VGS"
        skoleOrganisasjon.skole[0].trinn[1].niva == "2VGS"
        skoleOrganisasjon.skole[1].trinn.size() == 2
        skoleOrganisasjon.skole[1].trinn[0].niva == "1VGS"
        skoleOrganisasjon.skole[1].trinn[1].niva == "2VGS"
    }

    def "Add SkoleOrganisasjon, Skole, Trinn and Basisgrupper. Then test Skole-structure"() {
        given:
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("skoleOrganisasjonResources.json").getFile().text))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("skoleResources.json").getFile().text))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("skoleResources.json").getFile().text))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("aarstrinnResources.json").getFile().text))
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("basisgrupperResources.json").getFile().text))
        when:
        SkoleOrganisasjon skoleOrganisasjon = new SkoleOrganisasjon()
        fintserviceTestComponents.addOrganisationInfo(skoleOrganisasjon, "testBearer")
        skoleOrganisasjon.skole = fintserviceTestComponents.getSkoleList("testBearer")
        fintserviceTestComponents.setSchoolLevelsAndGroups(skoleOrganisasjon, "testBearer")

        then:
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper.size() == 2
        skoleOrganisasjon.skole[0].trinn[1].basisgrupper.size() == 1
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper[0].navn == "1STA"
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper[1].navn == "1OLA"
        skoleOrganisasjon.skole[0].trinn[1].basisgrupper[0].navn == "2OLA"
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper[0].antall == 4
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper[1].antall == 4
        skoleOrganisasjon.skole[0].trinn[1].basisgrupper[0].antall == 4
        skoleOrganisasjon.skole[1].trinn[0].basisgrupper.size() == 1
        skoleOrganisasjon.skole[1].trinn[1].basisgrupper.size() == 1
        skoleOrganisasjon.skole[1].trinn[0].basisgrupper[0].navn == "1LAT"
        skoleOrganisasjon.skole[1].trinn[1].basisgrupper[0].navn == "2LAT"
        skoleOrganisasjon.skole[1].trinn[0].basisgrupper[0].antall == 4
        skoleOrganisasjon.skole[1].trinn[1].basisgrupper[0].antall == 4
    }

    def "When server responds HTTP.NOT_FOUND cast URINotFoundException"() {
        given:
        server.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()))

        when:
        Exception errorException = new Exception()
        try {
            AbstractCollection abstractCollection = fintserviceTestComponents.getSkoleList("testBearer")
        } catch (Exception e) {
            errorException = e
        }
        then:
        errorException instanceof URINotFoundException
    }

    def "When URI is wrong cast URINotFoundException"() {
        given:
        String a404URI = "https://play-with-fint.felleskomponent.no/administrasjon/organisasjon/organisasjonselementer"

        when:
        Exception errorException = new Exception()
        OrganisasjonselementResources organisasjonselementResources = new OrganisasjonselementResources()
        try {
            AbstractCollection abstractCollection = fintserviceTestComponents.getResources(a404URI, "testBearer", organisasjonselementResources)
        } catch (Exception e) {
            errorException = e
        }
        then:
        errorException instanceof URINotFoundException
    }

    def "When request not answered within {time-out-time} cast ResourceRequestTimeoutException"() {
        given:
        def build = WebClient.builder().clientConnector(fintserviceTestComponents.tcp()).baseUrl(server.url('/').toString()).build()
        def fintserviceTestComponents2 = new FintService(webClient: build)
        when:
        Exception errorException = new Exception()
        OrganisasjonselementResources organisasjonselementResources = new OrganisasjonselementResources()
        try {
            organisasjonselementResources = fintserviceTestComponents2.getResources(fintserviceTestComponents.GET_ORGANISATION_URI, "testBearer", organisasjonselementResources)
        } catch (Exception e) {
            errorException = e
        }
        then:
        errorException instanceof ResourceRequestTimeoutException
    }

    /* def "Test cast ResourceRequestTimeoutException to Controller"() {
         given:
         ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration()
         def client = applicationConfiguration.webClient()
         def fintservice = new FintService(webClient: client)
         when:
         Exception errorException = new Exception()
         OrganisasjonselementResources organisasjonselementResources = new OrganisasjonselementResources()
         organisasjonselementResources = fintservice.getResources(fintservice.GET_ORGANISATION_URI, "testBearer", organisasjonselementResources)

         then:
         print("Hipp")
     }*/
}