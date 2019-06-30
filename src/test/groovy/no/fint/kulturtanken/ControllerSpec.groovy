package no.fint.kulturtanken


import no.fint.test.utils.MockMvcSpecification
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.reactive.function.client.WebClient

class ControllerSpec extends MockMvcSpecification {

    private MockMvc mockMvc
    private FintService fintService
    private Controller controller
    private WebClient webClient = Mock(WebClient)

    void setup() {
        fintService = new FintService(webClient: webClient)
        controller = new Controller(fintService: fintService)
        mockMvc = standaloneSetup(controller)
    }

    def "When some of the backend data returns 404 the controller should return 404 "() {

        when:
        def response = mockMvc.perform(get('api/skoleorganisasjon'))

        then:
        0 * webClient.get() >> new URINotFoundException("test")
        response.andExpect(status().isNotFound())
    }
}
