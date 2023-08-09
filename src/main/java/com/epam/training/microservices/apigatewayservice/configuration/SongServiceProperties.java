package com.epam.training.microservices.apigatewayservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = SongServiceProperties.PREFIX)
public class SongServiceProperties {
  public static final String PREFIX = "song.service";
  private String name;
  private String path;
  private String uri;
  private String byResourceId;

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  public String getUri() {
    return uri;
  }

  public String getByResourceId() {
    return byResourceId;
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

  public void setByResourceId(String byResourceId) {
    this.byResourceId = byResourceId;
  }
}
