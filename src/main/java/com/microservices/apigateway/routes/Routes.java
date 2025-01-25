package com.microservices.apigateway.routes;

import com.microservices.apigateway.config.RoutesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class Routes {

    private final RoutesProperties routesProperties;

    @Bean
    public RouteLocator gameServiceRoute(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("game-route", r -> r.path("/api/game/**")
                        .filters(f -> f.stripPrefix(1))
//                                .circuitBreaker(c -> c.setName("gameServiceCircuitBreaker")
//                                        .setFallbackUri("forward:/fallbackRoute")))
                        .uri(routesProperties.getGameUrl()))
                .route("user-route", r -> r.path("/api/user/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(routesProperties.getUserUrl()))
                .route("auth-route", r -> r.path("/api/auth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(routesProperties.getAuthUrl()))
                .build();
    }
}
