package com.epam.training.microservices.apigatewayservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "rate-limiter")
@ConstructorBinding
public class RateLimiterProperties {
  private final int replenishRate;
  private final int burstCapacity;
  private final int requestedTokens;

  public RateLimiterProperties(int replenishRate, int burstCapacity, int requestedTokens) {
    this.replenishRate = replenishRate;
    this.burstCapacity = burstCapacity;
    this.requestedTokens = requestedTokens;
  }

  public int getReplenishRate() {
    return replenishRate;
  }

  public int getBurstCapacity() {
    return burstCapacity;
  }

  public int getRequestedTokens() {
    return requestedTokens;
  }
}
