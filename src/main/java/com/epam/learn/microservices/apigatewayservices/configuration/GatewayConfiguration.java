package com.epam.learn.microservices.apigatewayservices.configuration;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfiguration {
    @Bean
    KeyResolver userKeyResolver() {
        return exchange -> Mono.just("1");
    }
}
