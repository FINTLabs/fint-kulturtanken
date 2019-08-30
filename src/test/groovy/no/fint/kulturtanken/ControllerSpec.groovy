package no.fint.kulturtanken


import no.fint.test.utils.MockMvcSpecification
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.reactive.function.client.WebClient

class ControllerSpec extends MockMvcSpecification {

    private MockMvc mockMvc
    private KulturtankenService kulturtankenService
    private def server = new MockWebServer()
    private Controller controller

    void setup() {
        kulturtankenService = new KulturtankenService(webClient: WebClient.create(server.url('/').toString()))
        controller = new Controller(kulturtankenService: kulturtankenService)
        mockMvc = standaloneSetup(controller)
    }

    def "When some of the backend data returns 404 the controller should return 404 "() {
        given:
        server.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()))

        when:
        def response = mockMvc.perform(get('/skoleeier').header(HttpHeaders.AUTHORIZATION, "bearer"))

        then:

        response.andExpect(status().isNotFound())
    }

    def "When some of the backend services times out return 408"() {
        given:
        server.enqueue(new MockResponse().setResponseCode(HttpStatus.REQUEST_TIMEOUT.value()))

        when:
        def response = mockMvc.perform(get('/skoleeier').header(HttpHeaders.AUTHORIZATION, "bearer"))

        then:

        response.andExpect(status().isRequestTimeout())
    }
}
