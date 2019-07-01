package no.fint.kulturtanken

import no.fint.ApplicationConfiguration
import no.fint.test.utils.MockMvcSpecification
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class ControllerSpec extends MockMvcSpecification {

    private MockMvc mockMvc
    private FintService fintService
    private Controller controller
    private WebClient webClient,webClient1
    private ApplicationConfiguration applicationConfiguration

    void setup() {
        applicationConfiguration = new ApplicationConfiguration()
        webClient1 = applicationConfiguration.webClient()
        webClient = Mock(WebClient)
        fintService = new FintService(webClient: webClient1)
        controller = new Controller(fintService: fintService)
        mockMvc = standaloneSetup(controller)
    }

    def "When some of the backend data returns 404 the controller should return 404 "() {
        when:
        def response = mockMvc.perform(get('/api/skoleorganisasjon/')
                .header(HttpHeaders.AUTHORIZATION, "minAuth"))
        then:
        0 * fintService.webClient.get() >> new URINotFoundException("test")
        response.andExpect(status().isNotFound())
    }
    def "When some backend returns 400 controller should return 400 Bad Request"() {

        when:
        def response = mockMvc.perform(get('/api/skoleorganisasjon/create'))

        then:
        //0 * webClient.get() >> new UnableToCreateResourceException("test")
        response.andExpect(status().isBadRequest())
    }
    def "When some backend returns 408 controller should return 408 Request Timeout"() {

        when:
        def response = mockMvc.perform(get('/api/skoleorganisasjon/timeout'))

        then:
        0 * webClient.get() >> new ResourceRequestTimeoutException("test")
        response.andExpect(status().isRequestTimeout())
    }
    def "With When some backend returns 408 controller should return 408 Request Timeout"() {

        when:
        def response = mockMvc.perform(get('http://localhost:8080/api/skoleorganisasjon/timeout'))

        then:
        0 * webClient.get() >> new ResourceRequestTimeoutException("test")
        response.andExpect(status().isRequestTimeout())
    }

}
