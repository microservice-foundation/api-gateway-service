package com.epam.training.microservices.apigatewayservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = RateLimiterProperties.PREFIX)
public class RateLimiterProperties {
  public static final String PREFIX = "rate-limiter";
  private int replenishRate;
  private int burstCapacity;
  private int requestedTokens;

  public int getReplenishRate() {
    return replenishRate;
  }

  public int getBurstCapacity() {
    return burstCapacity;
  }

  public int getRequestedTokens() {
    return requestedTokens;
  }

  public void setReplenishRate(int replenishRate) {
    this.replenishRate = replenishRate;
  }

  public void setBurstCapacity(int burstCapacity) {
    this.burstCapacity = burstCapacity;
  }

  public void setRequestedTokens(int requestedTokens) {
    this.requestedTokens = requestedTokens;
  }
}
