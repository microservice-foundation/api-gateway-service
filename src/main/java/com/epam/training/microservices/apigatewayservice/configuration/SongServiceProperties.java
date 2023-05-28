package com.epam.training.microservices.apigatewayservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "song.service")
@ConstructorBinding
public class SongServiceProperties {
  private final String name;
  private final String path;
  private final String uri;
  private final String byResourceId;

  public SongServiceProperties(String name, String path, String uri, String byResourceId) {
    this.name = name;
    this.path = path;
    this.uri = uri;
    this.byResourceId = byResourceId;
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

  public String getByResourceId() {
    return byResourceId;
  }
}
