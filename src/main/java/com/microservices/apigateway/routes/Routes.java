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
                .route("game-route", r -> r.path("/game/**")
                        .filters(f -> f.stripPrefix(1))
//                                .circuitBreaker(c -> c.setName("gameServiceCircuitBreaker")
//                                        .setFallbackUri("forward:/fallbackRoute")))
                        .uri("http://localhost:8080"))
                .build();
    }

    @Bean
    public RouteLocator userServiceRoute(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-route", r -> r.path("/user/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8081"))
                .build();
    }

    @Bean
    public RouteLocator authServiceRoute(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-route", r -> r.path("/auth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://localhost:8082"))
                .build();
    }
}
