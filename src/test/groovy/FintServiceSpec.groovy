import no.fint.kulturtanken.FintServiceTestComponents
import no.fint.kulturtanken.model.SkoleOrganisasjon
import no.fint.model.felles.kompleksedatatyper.Kontaktinformasjon
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

class FintServiceSpec extends Specification {
    private def server = new MockWebServer()
    private def webClient = WebClient.create(server.url('/').toString())
    private def fintserviceTestComponents = new FintServiceTestComponents(webClient: webClient)


    def "Get OrganisasjonselementResources from webclient and build SkoleOrganisasjon"() {
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

    def "Atempt to create SkoleOrganisasjon, but return blank if getOrganisasjonselementResources returns empty"() {
        given:
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("skoleOrganisasjonResourcesEmpty.json").getFile().text))
        when:
        SkoleOrganisasjon skoleOrganisasjon = fintserviceTestComponents.getSkoleOrganisasjon("testBearer")

        then:
        skoleOrganisasjon == null
    }

    def "Get ContactInformation and check if extracts email and phone number to new Object"() {
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

    def "Get SkoleResources from webclient and build 2 Skoler with no Levels"() {
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
    def "Get SkoleOrganisasjon, Skoler, Arstrinn and Basisgrupper from webclient check that levels are initated and correct"() {
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

    def "Get SkoleOrganisasjon, Skoler, Arstrinn and Basisgrupper from webclient check that groups are initated and correct"() {
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

    def "Get SkoleOrganisasjon, Skoler, Arstrinn and Basisgrupper from webclient and build levels and groups correct"() {
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
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper.size() == 2
        skoleOrganisasjon.skole[0].trinn[1].basisgrupper.size() == 1
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper[0].navn == "1STA"
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper[1].navn == "1OLA"
        skoleOrganisasjon.skole[0].trinn[1].basisgrupper[0].navn == "2OLA"
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper[0].antall == 4
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper[1].antall == 4
        skoleOrganisasjon.skole[0].trinn[1].basisgrupper[0].antall == 4
        skoleOrganisasjon.skole[1].trinn.size() == 2
        skoleOrganisasjon.skole[1].trinn[0].niva == "1VGS"
        skoleOrganisasjon.skole[1].trinn[1].niva == "2VGS"
        skoleOrganisasjon.skole[1].trinn[0].basisgrupper.size() == 1
        skoleOrganisasjon.skole[1].trinn[1].basisgrupper.size() == 1
        skoleOrganisasjon.skole[1].trinn[0].basisgrupper[0].navn == "1LAT"
        skoleOrganisasjon.skole[1].trinn[1].basisgrupper[0].navn == "2LAT"
        skoleOrganisasjon.skole[1].trinn[0].basisgrupper[0].antall == 4
        skoleOrganisasjon.skole[1].trinn[1].basisgrupper[0].antall == 4
    }
}