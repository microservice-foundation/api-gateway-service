package com.epam.training.microservices.apigatewayservice.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.springframework.http.HttpStatus;

public final class MockServer {
  private final MockWebServer server;
  private final ObjectMapper mapper;

  private MockServer() {
    mapper = new ObjectMapper();
    server = new MockWebServer();
  }

  private MockServer(int port) throws IOException {
    this();
    server.start(port);
  }

  private MockServer(InetAddress address, int port) throws IOException {
    this();
    server.start(address, port);
  }

  public <T> void responseWithJson(HttpStatus status, T responseBody, Map<String, String> headers) {
    MockResponse response = new MockResponse();
    response.setResponseCode(status.value());
    response.setBody(toJson(responseBody));
    headers.forEach(response::addHeader);
    server.enqueue(response);
  }

  public void responseWithBuffer(HttpStatus status, Buffer responseBody, Map<String, String> headers) {
    MockResponse response = new MockResponse();
    response.setResponseCode(status.value());
    response.setBody(responseBody);
    headers.forEach(response::addHeader);
    server.enqueue(response);
  }

  public <T> void response(HttpStatus status) {
    MockResponse response = new MockResponse();
    response.setResponseCode(status.value());
    server.enqueue(response);
  }

  private <T> String toJson(T value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void dispose() throws IOException {
    server.close();
  }

  public static MockServer newInstance() {
    return new MockServer();
  }

  public static MockServer newInstance(int port) throws IOException {
    return new MockServer(port);
  }

  public static MockServer newInstance(InetAddress inetAddress, int port) throws IOException {
    return new MockServer(inetAddress, port);
  }

}
