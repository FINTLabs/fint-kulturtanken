package no.fint.kulturtanken.repository

import com.fasterxml.jackson.databind.ObjectMapper
import no.fint.kulturtanken.configuration.KulturtankenProperties
import no.fint.kulturtanken.util.FintObjectFactory
import no.fint.model.resource.Link
import no.fint.model.resource.utdanning.elev.BasisgruppeResources
import no.fint.model.resource.utdanning.timeplan.FagResources
import no.fint.model.resource.utdanning.timeplan.UndervisningsgruppeResources
import no.fint.model.resource.utdanning.utdanningsprogram.ArstrinnResources
import no.fint.model.resource.utdanning.utdanningsprogram.SkoleResources
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import spock.lang.Specification

class FintRepositorySpec extends Specification {
    MockWebServer mockWebServer = new MockWebServer()
    WebClient webClient

    OAuth2AuthorizedClientManager authorizedClientManager = Mock {
        1 * authorize(_) >> Mock(OAuth2AuthorizedClient)
    }

    KulturtankenProperties kulturtankenProperties = Mock {
        1 * getOrganisations() >> [(_ as String): new KulturtankenProperties.Organisation(organisationNumber: _ as String, environment: _ as String, username: _ as String, password: _ as String)]
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
                .setResponseCode(400))

        when:
        def resources = fintRepository.getSchools(_ as String)

        then:
        thrown(WebClientResponseException)
        //resources.getTotalItems() == 1
        //resources.getContent().get(0).navn == 'School'
    }

    def "Get basis groups from Fint"() {
        given:
        def basisGroup = FintObjectFactory.newBasisGroup()
        def basisGroups = new BasisgruppeResources()
        basisGroups.addResource(basisGroup)
        mockWebServer.enqueue(new MockResponse()
                .setBody(new ObjectMapper().writeValueAsString(basisGroups))
                .setHeader('content-type', 'application/json')
                .setResponseCode(200))

        when:
        def resources = fintRepository.getBasisGroups(_ as String)

        then:
        resources.size() == 1
        resources.get(Link.with('link.To.BasisGroup')).navn == 'Basis group'
    }

    def "Get levels from Fint"() {
        given:
        def level = FintObjectFactory.newLevel()
        def levels = new ArstrinnResources()
        levels.addResource(level)
        mockWebServer.enqueue(new MockResponse()
                .setBody(new ObjectMapper().writeValueAsString(levels))
                .setHeader('content-type', 'application/json')
                .setResponseCode(200))

        when:
        def resources = fintRepository.getLevels(_ as String)

        then:
        resources.size() == 1
        resources.get(Link.with('link.To.Level')).navn == 'Level'
    }

    def "Get teaching groups from Fint"() {
        given:
        def teachingGroup = FintObjectFactory.newTeachingGroup()
        def teachingGroups = new UndervisningsgruppeResources()
        teachingGroups.addResource(teachingGroup)
        mockWebServer.enqueue(new MockResponse()
                .setBody(new ObjectMapper().writeValueAsString(teachingGroups))
                .setHeader('content-type', 'application/json')
                .setResponseCode(200))

        when:
        def resources = fintRepository.getTeachingGroups(_ as String)

        then:
        resources.size() == 1
        resources.get(Link.with('link.To.TeachingGroup')).navn == 'Teaching group'
    }

    def "Get subjects from Fint"() {
        given:
        def subject = FintObjectFactory.newSubject()
        def subjects = new FagResources()
        subjects.addResource(subject)
        mockWebServer.enqueue(new MockResponse()
                .setBody(new ObjectMapper().writeValueAsString(subjects))
                .setHeader('content-type', 'application/json')
                .setResponseCode(200))

        when:
        def resources = fintRepository.getSubjects(_ as String)

        then:
        resources.size() == 1
        resources.get(Link.with('link.To.Subject')).navn == 'Subject'
    }
}
