package com.microservices.apigateway.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Routes {

    @Bean
    public RouteLocator gameServiceRoute(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("game-route", r -> r.path("/api/game/**")
                        .filters(f -> f.stripPrefix(1))
//                                .circuitBreaker(c -> c.setName("gameServiceCircuitBreaker")
//                                        .setFallbackUri("forward:/fallbackRoute")))
                        .uri("http://localhost:8080"))
                .route("user-route", r -> r.path("/api/user/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8081"))
                .route("auth-route", r -> r.path("/api/auth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8082"))
                .build();
    }
}
