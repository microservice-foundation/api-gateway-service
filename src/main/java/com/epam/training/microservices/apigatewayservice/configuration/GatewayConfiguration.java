package com.epam.training.microservices.apigatewayservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(value = {ResourceServiceProperties.class, SongServiceProperties.class, RateLimiterProperties.class})
public class GatewayConfiguration {

  @Value("${api-gateway.service.uri}")
  private String baseUrl;

  @Bean
  KeyResolver userKeyResolver() {
    return exchange -> Mono.just("1");
  }

  @Bean
  public WebClient webClient(WebClient.Builder webClientBuilder,
      ReactorLoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction) {
    return webClientBuilder
        .baseUrl(baseUrl)
        .filter(loadBalancerExchangeFilterFunction)
        .build();
  }
}
