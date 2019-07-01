package no.fint.kulturtanken

import no.fint.test.utils.MockMvcSpecification
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc

class ControllerSpec extends MockMvcSpecification {

    private MockMvc mockMvc
    private FintService mockService
    private Controller controller

    void setup() {
        mockService = Mock(FintService)
        controller = new Controller(fintService: mockService)
        mockMvc = standaloneSetup(controller)
    }

    def "When some of the backend data returns 404 the controller should return 404 "() {
        given:
        1 * mockService.getSkoleOrganisasjon("test") >> { throw new URINotFoundException("test") }

        when:
        def response = mockMvc.perform(get('/api/skoleorganisasjon/')
                .header(HttpHeaders.AUTHORIZATION, "test"))

        then:
        response.andExpect(status().isNotFound())
    }

    def "When some backend returns 400 controller should return 400 Bad Request"() {
        given:
        1 * mockService.getSkoleOrganisasjon("test") >> { throw new UnableToCreateResourceException("test") }

        when:
        def response = mockMvc.perform(get('/api/skoleorganisasjon/')
                .header(HttpHeaders.AUTHORIZATION, "test"))

        then:
        response.andExpect(status().isBadRequest())
    }

    def "When some backend returns 408 controller should return 408 Request Timeout"() {
        given:
        1 * mockService.getSkoleOrganisasjon("test") >> { throw new ResourceRequestTimeoutException("test") }

        when:
        def response = mockMvc.perform(get('/api/skoleorganisasjon/')
                .header(HttpHeaders.AUTHORIZATION, "test"))

        then:
        response.andExpect(status().isRequestTimeout())
    }
}
