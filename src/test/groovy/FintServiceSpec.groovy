import no.fint.kulturtanken.FintServiceTestComponents
import no.fint.model.resource.administrasjon.organisasjon.OrganisasjonselementResources
import no.fint.model.resource.utdanning.elev.BasisgruppeResources
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources
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


    def "Get OrganisasjonselementResources from webclient and build SkoleOrganisasjon"(){
        given:
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("skoleOrganisasjonResources.json").getFile().text))
        when:
        OrganisasjonselementResources reply = fintserviceTestComponents.getOrganisasjonselementResources("test")

        then:
        print(reply.toString())
    }

    def "Get SkoleResources from webclient and build Skole"(){
        given:
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("skoleResources.json").getFile().text))
        when:
        SkoleResources reply = fintserviceTestComponents.getSkoleResources("test")

        then:
        print(reply.toString())
    }

    def "Get basisgrupper from webclient and build basisgruppe"(){
        given:
        server.enqueue(
                new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(new ClassPathResource("basisgrupperResources.json").getFile().text))
        when:
        BasisgruppeResources reply = fintserviceTestComponents.getBasisgruppeResources("test")

        then:
        print(reply.getContent().toString())
    }

    def "Get skoleOrganisasjonTestData check school size is four"() {
        given:

        when:
        def skoleOrganisasjon = fintserviceTestComponents.skoleOrganisasjonTestData

        then:
        skoleOrganisasjon.skole.size() == 2
    }
    def "Get skoleOrganisasjonTestData check school level size is three and zero"() {
        given:

        when:
        def skoleOrganisasjon = fintserviceTestComponents.skoleOrganisasjonTestData

        then:
        skoleOrganisasjon.skole[0].trinn.size() == 3
        skoleOrganisasjon.skole[1].trinn.size() == 0
    }

    def "Get skoleOrganisasjonTestData check if school basisgruppe has correct amount"() {
        given:

        when:
        def skoleOrganisasjon = fintserviceTestComponents.skoleOrganisasjonTestData

        then:
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper.size() == 2
        skoleOrganisasjon.skole[0].trinn[1].basisgrupper.size() == 1
        skoleOrganisasjon.skole[0].trinn[2].basisgrupper.size() == 1
    }
    def "Get skoleOrganisasjonTestData check if school basisgruppe has students"() {
        given:

        when:
        def skoleOrganisasjon = fintserviceTestComponents.skoleOrganisasjonTestData

        then:
        skoleOrganisasjon.skole[0].trinn[0].basisgrupper.each {
            it.antall>0
        }
    }

}