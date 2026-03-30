package com.ms.apigateway

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

/**
 * Tests for Circuit Breaker configuration in routes
 */
@SpringBootTest
@ActiveProfiles("test")
class CircuitBreakerSpec extends Specification {

    @Autowired
    RouteLocator routeLocator

    def "should configure CircuitBreaker filter for route: #routeId"() {
        when: "getting route"
            def routes = routeLocator.routes.collectList().block()
            def route = routes.find { it.id == routeId }

        then: "route should exist"
            route != null

        and: "should have CircuitBreaker filter configured"
            def circuitBreakerFilter = route.filters.find { filter ->
                filter.toString().contains("CircuitBreaker") || filter.toString().contains("circuitbreaker")
            }
            circuitBreakerFilter != null

        where:
            routeId << ["game-route", "user-route", "auth-route"]
    }

    def "should configure FallbackHeaders filter for route: #routeId"() {
        when: "getting route"
            def routes = routeLocator.routes.collectList().block()
            def route = routes.find { it.id == routeId }

        then: "route should exist"
            route != null

        and: "should have FallbackHeaders filter configured"
            def fallbackFilter = route.filters.find { filter ->
                filter.toString().contains("FallbackHeaders") || filter.toString().contains("fallback")
            }
            fallbackFilter != null

        where:
            routeId << ["game-route", "user-route", "auth-route"]
    }

    def "all routes should have resilience filters configured"() {
        when: "getting all routes"
            def routes = routeLocator.routes.collectList().block()

        then: "all routes should have at least 4 filters (1 default + 3 custom)"
            routes.every { route ->
                route.filters.size() >= 4
            }

        and: "all routes should have CircuitBreaker configured"
            routes.every { route ->
                route.filters.any { filter ->
                    filter.toString().toLowerCase().contains("circuit") ||
                            filter.toString().toLowerCase().contains("breaker")
                }
            }
    }

    def "routes should have correct filter order"() {
        when: "getting game route"
            def routes = routeLocator.routes.collectList().block()
            def gameRoute = routes.find { it.id == "game-route" }

        then: "should have DedupeResponseHeader as first filter (from default-filters)"
            gameRoute.filters[0].toString().contains("DedupeResponseHeader")

        and: "should have StripPrefix as second filter"
            gameRoute.filters[1].toString().contains("StripPrefix")

        and: "should have CircuitBreaker filter"
            gameRoute.filters.any { it.toString().contains("CircuitBreaker") }

        and: "should have at least 4 filters total (default + 3 custom)"
            gameRoute.filters.size() >= 4
    }
}
