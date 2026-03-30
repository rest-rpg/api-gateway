package com.ms.apigateway

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import spock.lang.Specification

/**
 * Spock tests for FallbackController
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class FallbackControllerSpec extends Specification {

    @Autowired
    WebTestClient webTestClient

    def "should return 503 with fallback response for any service"() {
        given: "a service name"
            def serviceName = "game"

        when: "calling the fallback endpoint"
            def response = webTestClient.get()
                    .uri("/fallback/{serviceName}", serviceName)
                    .exchange()

        then: "should return 503 Service Unavailable"
            response.expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)

        and: "response contains error details"
            response.expectBody()
                    .jsonPath('$.status').isEqualTo(503)
                    .jsonPath('$.error').isEqualTo("Service Unavailable")
                    .jsonPath('$.message').value({ String msg ->
                assert msg.contains(serviceName)
                assert msg.contains("temporarily unavailable")
            })
                    .jsonPath('$.service').isEqualTo(serviceName)
                    .jsonPath('$.method').isEqualTo("GET")
                    .jsonPath('$.timestamp').exists()
    }

    def "should handle POST requests to fallback endpoint"() {
        when: "calling fallback with POST method"
            def response = webTestClient.post()
                    .uri("/fallback/user")
                    .exchange()

        then: "should return 503"
            response.expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)

        and: "method should be POST"
            response.expectBody()
                    .jsonPath('$.method').isEqualTo("POST")
                    .jsonPath('$.service').isEqualTo("user")
    }

    def "should include exception details when headers are present"() {
        given: "exception headers"
            def exceptionType = "java.net.ConnectException"
            def exceptionMessage = "Connection refused"

        when: "calling fallback with exception headers"
            def response = webTestClient.get()
                    .uri("/fallback/auth")
                    .header("Exception-Type", exceptionType)
                    .header("Exception-Message", exceptionMessage)
                    .exchange()

        then: "should include exception details in response"
            response.expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            response.expectBody()
                    .jsonPath('$.exceptionType').isEqualTo(exceptionType)
                    .jsonPath('$.exceptionMessage').isEqualTo(exceptionMessage)
    }

    def "should work with any service name"() {
        expect: "fallback to work for different service names"
            webTestClient.get()
                    .uri("/fallback/{serviceName}", serviceName)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                    .expectBody()
                    .jsonPath('$.service').isEqualTo(serviceName)

        where:
            serviceName << ["game", "user", "auth", "payment", "inventory", "notification"]
    }

    def "should handle HTTP methods: #httpMethod"() {
        when: "calling fallback with different HTTP methods"
            def response = webTestClient.method(httpMethod)
                    .uri("/fallback/test-service")
                    .exchange()

        then: "should return 503 for all methods"
            response.expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            response.expectBody()
                    .jsonPath('$.method').isEqualTo(httpMethod.name())

        where:
            httpMethod << [
                    org.springframework.http.HttpMethod.GET,
                    org.springframework.http.HttpMethod.POST,
                    org.springframework.http.HttpMethod.PUT,
                    org.springframework.http.HttpMethod.DELETE,
                    org.springframework.http.HttpMethod.PATCH
            ]
    }
}
