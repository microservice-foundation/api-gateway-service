package com.epam.training.microservices.apigatewayservice.common;

import static com.epam.training.microservices.apigatewayservice.common.Server.Service.RESOURCE;
import static com.epam.training.microservices.apigatewayservice.common.Server.Service.SONG;
import static com.epam.training.microservices.apigatewayservice.common.Server.Service.STORAGE;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

public class MockServerExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {

  private Map<Server.Service, MockServer> serverMap;
  private Properties properties;

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();
    Server serverAnnotation = parameter.getDeclaredAnnotation(Server.class);
    return serverMap.containsKey(serverAnnotation.service());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();
    Server serverAnnotation = parameter.getDeclaredAnnotation(Server.class);
    return serverMap.get(serverAnnotation.service());
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    for(MockServer mockServer: serverMap.values()) {
      mockServer.dispose();
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    String resourcePort = properties.getProperty("resource.service.port");
    MockServer resourceMockServer = MockServer.newInstance(Integer.parseInt(resourcePort));

    String songServicePort = properties.getProperty("song.service.port");
    MockServer songMockServer = MockServer.newInstance(Integer.parseInt(songServicePort));

    String storageServicePort = properties.getProperty("storage.service.port");
    MockServer storageMockServer = MockServer.newInstance(Integer.parseInt(storageServicePort));

    serverMap = new HashMap<>();
    serverMap.put(RESOURCE, resourceMockServer);
    serverMap.put(SONG, songMockServer);
    serverMap.put(STORAGE, storageMockServer);
  }

  private Properties loadApplicationProperties() throws IOException {
    ClassPathResource resource = new ClassPathResource("application.properties");
    return PropertiesLoaderUtils.loadProperties(resource);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    properties = loadApplicationProperties();
  }
}
