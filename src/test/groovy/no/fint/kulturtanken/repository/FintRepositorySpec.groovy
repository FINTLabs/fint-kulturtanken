package no.fint.kulturtanken.repository

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.kulturtanken.kulturanken.KulturtankenProperties
import no.fint.kulturtanken.util.FintObjectFactory
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

class FintRepositorySpec extends Specification {
    MockWebServer mockWebServer = new MockWebServer()
    WebClient webClient

    OAuth2AuthorizedClientManager authorizedClientManager = Mock {
        1 * authorize(_) >> Mock(OAuth2AuthorizedClient)
    }

    KulturtankenProperties kulturtankenProperties = Mock {
        1 * getOrganisations() >> [(_ as String): new KulturtankenProperties.Organisation(organisationNumber: _ as String,
                registration: [(_ as String): new KulturtankenProperties.Registration(id: _ as String, environment: _ as String,
                        username: _ as String, password: _ as String)])]
    }

    FintRepository fintRepository

    void setup() {
        webClient = WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build()
        fintRepository = new FintRepository(webClient, Mock(Authentication), authorizedClientManager, kulturtankenProperties)
    }

    def "Get schools from Fint"() {
        given:
        def school = FintObjectFactory.newSchool()
        def schools = new SkoleResources()
        schools.addResource(school)
        mockWebServer.enqueue(new MockResponse()
                .setBody(new ObjectMapper().writeValueAsString(schools))
                .setHeader('content-type', 'application/json')
                .setResponseCode(200))

        when:
        def resources = fintRepository.getResources(_ as String, SkoleResources.class)

        then:
        resources.blockLast().navn == 'School'
    }
}
