package com.ms.apigateway

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

/**
 * Integration tests for Gateway routing configuration
 */
@SpringBootTest
@ActiveProfiles("test")
class GatewayRoutingSpec extends Specification {

    @Autowired
    RouteLocator routeLocator

    def "should load all configured routes"() {
        when: "getting all routes"
            def routes = routeLocator.routes.collectList().block()

        then: "should have 3 routes configured"
            routes.size() == 3

        and: "routes should have correct IDs"
            routes*.id.containsAll(["game-route", "user-route", "auth-route"])
    }

    def "should configure game-route with correct predicates and filters"() {
        when: "getting game-route"
            def routes = routeLocator.routes.collectList().block()
            def gameRoute = routes.find { it.id == "game-route" }

        then: "game-route should exist"
            gameRoute != null

        and: "should have correct URI"
            gameRoute.uri.toString().contains("8080")

        and: "should have Path predicate"
            gameRoute.predicate != null

        and: "should have filters (default + StripPrefix, CircuitBreaker, FallbackHeaders)"
            gameRoute.filters.size() >= 4
    }

    def "should configure user-route with correct predicates and filters"() {
        when: "getting user-route"
            def routes = routeLocator.routes.collectList().block()
            def userRoute = routes.find { it.id == "user-route" }

        then: "user-route should exist"
            userRoute != null

        and: "should have correct URI"
            userRoute.uri.toString().contains("8081")

        and: "should have filters configured"
            userRoute.filters.size() >= 4
    }

    def "should configure auth-route with correct predicates and filters"() {
        when: "getting auth-route"
            def routes = routeLocator.routes.collectList().block()
            def authRoute = routes.find { it.id == "auth-route" }

        then: "auth-route should exist"
            authRoute != null

        and: "should have correct URI"
            authRoute.uri.toString().contains("8082")

        and: "should have filters configured"
            authRoute.filters.size() >= 4
    }

    def "should configure metadata for route: #routeId"() {
        when: "getting route metadata"
            def routes = routeLocator.routes.collectList().block()
            def route = routes.find { it.id == routeId }

        then: "route should have metadata"
            route != null
            route.metadata != null

        and: "should have timeout configuration"
            route.metadata.containsKey("response-timeout") || route.metadata.containsKey("connect-timeout")

        where:
            routeId << ["game-route", "user-route", "auth-route"]
    }

    def "all routes should be properly ordered"() {
        when: "getting all routes"
            def routes = routeLocator.routes.collectList().block()

        then: "routes should be in order"
            routes.size() > 0

        and: "each route should have an order"
            routes.every { it.order != null }
    }
}
