package com.microservices.apigateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "routes")
@Getter
@Setter
public class RoutesProperties {

    private String gameUrl;
    private String userUrl;
    private String authUrl;
}
