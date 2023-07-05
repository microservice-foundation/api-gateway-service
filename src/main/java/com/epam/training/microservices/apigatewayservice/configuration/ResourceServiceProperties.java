package com.epam.training.microservices.apigatewayservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = ResourceServiceProperties.PREFIX)
public class ResourceServiceProperties {
  public static final String PREFIX = "resource.service";
  private String name;
  private String path;
  private String uri;

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public String getUri() {
    return uri;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }
}
