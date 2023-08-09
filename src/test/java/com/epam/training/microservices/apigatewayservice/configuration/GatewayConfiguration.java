package com.epam.training.microservices.apigatewayservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@TestConfiguration
@EnableConfigurationProperties(value = {ResourceServiceProperties.class, SongServiceProperties.class, StorageServiceProperties.class,
    RateLimiterProperties.class})
public class GatewayConfiguration {
  @Value("${song.service.uri}")
  private String baseUrl;

  @Bean
  KeyResolver userKeyResolver() {
    return exchange -> Mono.just("1");
  }

  @Bean
  public WebClient webClient() {
    return WebClient.builder()
        .baseUrl(baseUrl)
        .build();
  }
}
