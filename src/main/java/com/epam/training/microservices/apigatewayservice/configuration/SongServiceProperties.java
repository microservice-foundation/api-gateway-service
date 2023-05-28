package com.epam.training.microservices.apigatewayservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "song.service")
@ConstructorBinding
public class SongServiceProperties {
  private final String name;
  private final String path;
  private final String uri;
  private final String deleteByResourceId;

  public SongServiceProperties(String name, String path, String uri, String deleteByResourceId) {
    this.name = name;
    this.path = path;
    this.uri = uri;
    this.deleteByResourceId = deleteByResourceId;
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

  public String getDeleteByResourceId() {
    return deleteByResourceId;
  }
}
