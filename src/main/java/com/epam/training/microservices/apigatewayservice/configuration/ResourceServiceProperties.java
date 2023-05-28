package com.epam.training.microservices.apigatewayservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "resource.service")
@ConstructorBinding
public class ResourceServiceProperties {
  private final String name;
  private final String path;
  private final String uri;

  public ResourceServiceProperties(String name, String path, String uri) {
    this.name = name;
    this.path = path;
    this.uri = uri;
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public String getUri() {
    return uri;
  }
}
