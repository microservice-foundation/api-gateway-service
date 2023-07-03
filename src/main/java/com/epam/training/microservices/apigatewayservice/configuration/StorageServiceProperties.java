package com.epam.training.microservices.apigatewayservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = StorageServiceProperties.PREFIX)
public class StorageServiceProperties {
  public static final String PREFIX = "storage.service";
  private String name;
  private String path;
  private String uri;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }
}
