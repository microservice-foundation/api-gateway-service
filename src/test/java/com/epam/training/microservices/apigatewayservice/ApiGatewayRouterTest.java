package com.epam.training.microservices.apigatewayservice;

import static com.epam.training.microservices.apigatewayservice.common.Server.Service.RESOURCE;
import static com.epam.training.microservices.apigatewayservice.common.Server.Service.SONG;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import com.epam.training.microservices.apigatewayservice.common.APIError;
import com.epam.training.microservices.apigatewayservice.common.MockServer;
import com.epam.training.microservices.apigatewayservice.common.MockServerExtension;
import com.epam.training.microservices.apigatewayservice.common.Server;
import com.epam.training.microservices.apigatewayservice.configuration.GatewayConfiguration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import kotlin.jvm.functions.Function1;
import okio.Buffer;
import okio.Okio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.BodyInserters;

@ExtendWith(MockServerExtension.class)
@SpringBootTest
@AutoConfigureWebTestClient
@ContextConfiguration(classes = GatewayConfiguration.class)
@TestPropertySource(locations = "classpath:application.properties")
class ApiGatewayRouterTest {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void shouldGetResourceById(@Server(service = RESOURCE) MockServer mockServer) throws IOException {
    mockServer.responseWithBuffer(HttpStatus.OK, fileBuffer(),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE));

    webTestClient.get()
        .uri("/resources/{id}", 123L)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentLength(testFile().length());
  }

  @Test
  void shouldReturnBadRequestWhenGetResourceById(@Server(service = RESOURCE) MockServer mockServer) {
    APIError apiError = new APIError(HttpStatus.BAD_REQUEST, "Invalid request", new RuntimeException());
    mockServer.responseWithJson(HttpStatus.BAD_REQUEST, apiError,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.get()
        .uri("/resources/{id}", 123L)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request");
  }

  @Test
  void shouldReturnNotFoundWhenGetResourceById(@Server(service = RESOURCE) MockServer mockServer) {
    APIError apiError = new APIError(HttpStatus.NOT_FOUND, "Resource is not found", new RuntimeException());
    mockServer.responseWithJson(HttpStatus.NOT_FOUND, apiError,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.get()
        .uri("/resources/{id}", 123L)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo("NOT_FOUND")
        .jsonPath("$.message").isEqualTo("Resource is not found");
  }

  @Test
  void shouldReturnNotFoundResourceFileWhenGetResourceById(@Server(service = RESOURCE) MockServer mockServer) {
    APIError apiError = new APIError(HttpStatus.NOT_FOUND, "Resource file is not found", new RuntimeException());
    mockServer.responseWithJson(HttpStatus.NOT_FOUND, apiError,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.get()
        .uri("/resources/{id}", 123L)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo("NOT_FOUND")
        .jsonPath("$.message").isEqualTo("Resource file is not found");
  }

  @Test
  void shouldReturnDownloadFileErrorWhenGetResourceById(@Server(service = RESOURCE) MockServer mockServer) {
    APIError apiError = new APIError(HttpStatus.INTERNAL_SERVER_ERROR, "Resource file download has failed: ",
        new RuntimeException());

    mockServer.responseWithJson(HttpStatus.INTERNAL_SERVER_ERROR, apiError,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.get()
        .uri("/resources/{id}", 123L)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").value(containsString("Resource file download has failed"));
  }

  @Test
  void shouldSaveResource(@Server(service = RESOURCE) MockServer mockServer) throws IOException {
    long id = 1L;
    mockServer.responseWithJson(HttpStatus.CREATED, Collections.singletonMap("id", id),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", new FileSystemResource(testFile()));

    webTestClient.post().uri("/resources")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .exchange()
        .expectStatus().isCreated()
        .expectBody().jsonPath("$.id").isEqualTo(id);
  }

  @Test
  void shouldThrowValidationExceptionWhenSaveResource(@Server(service = RESOURCE) MockServer mockServer) throws IOException {
    mockServer.responseWithJson(HttpStatus.BAD_REQUEST, new APIError(HttpStatus.BAD_REQUEST, "Invalid request",
        new RuntimeException()), Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", testFile());

    webTestClient.post().uri("/resources")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request");
  }

  @Test
  void shouldThrowValidationExceptionWhenSaveResourceWithEmptyFile(@Server(service = RESOURCE) MockServer mockServer) {
    mockServer.responseWithJson(HttpStatus.BAD_REQUEST, new APIError(HttpStatus.BAD_REQUEST, "Invalid request",
        new RuntimeException()), Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", new byte[0]);

    webTestClient.post().uri("/resources")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request");
  }

  @Test
  void shouldThrowValidationExceptionWhenSaveInvalidTypeResource(@Server(service = RESOURCE) MockServer mockServer) {
    mockServer.responseWithJson(HttpStatus.BAD_REQUEST, new APIError(HttpStatus.BAD_REQUEST, "Invalid request",
        new RuntimeException()), Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", "this is string");

    webTestClient.post().uri("/resources")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid request");
  }


  @Test
  void shouldThrowUploadFailedExceptionWhenSaveResource(@Server(service = RESOURCE) MockServer mockServer) throws IOException {
    mockServer.responseWithJson(HttpStatus.INTERNAL_SERVER_ERROR, new APIError(HttpStatus.INTERNAL_SERVER_ERROR,
            "Resource file upload has failed", new RuntimeException()),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    MultiValueMap<String, Object> parts2 = new LinkedMultiValueMap<>();
    parts2.add("file", new FileSystemResource(testFile()));

    webTestClient.post().uri("/resources")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts2))
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").value(containsString("Resource file upload has failed"));

  }

  @Test
  void shouldDeleteResourceByIds(@Server(service = RESOURCE) MockServer resourceServiceServer,
      @Server(service = SONG) MockServer songServiceResource) {
    long[] resourceIds = {1L, 2L};
    List<Map<String, Object>> resourceRecords = new ArrayList<>();
    resourceRecords.add(Collections.singletonMap("id", resourceIds[0]));
    resourceRecords.add(Collections.singletonMap("id", resourceIds[1]));
    resourceServiceServer.responseWithJson(HttpStatus.OK, resourceRecords,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    long[] songMetadataIds = {100L, 101L};
    List<Map<String, Object>> songRecords = new ArrayList<>();
    songRecords.add(Map.of("id", songMetadataIds[0], "resourceId", resourceIds[0]));
    songRecords.add(Map.of("id", songMetadataIds[1], "resourceId", resourceIds[1]));
    songServiceResource.responseWithJson(HttpStatus.OK, songRecords,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/resources")
            .queryParam("id", resourceIds[0] + "," + resourceIds[1])
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[*].id").value(containsInAnyOrder(
            is((int)resourceIds[0]),
            is((int)resourceIds[1])
        ))
        .jsonPath("$[*].song_metadata.id").value(containsInAnyOrder(
            is((int)songMetadataIds[0]),
            is((int)songMetadataIds[1])
        ));
  }

  @Test
  void shouldThrowExceptionWhenDeleteResourceByEmptyIdsList(@Server(service = RESOURCE) MockServer resourceServiceServer) {
    resourceServiceServer.responseWithJson(HttpStatus.BAD_REQUEST, new APIError(HttpStatus.BAD_REQUEST,
            "Invalid Request", new RuntimeException("For input string: \"\"")),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/resources")
            .queryParam("id", "")
            .build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo("BAD_REQUEST")
        .jsonPath("$.message").isEqualTo("Invalid Request")
        .jsonPath("$.debugMessage").isEqualTo("For input string: \"\"");
  }

  @Test
  void shouldReturnEmptyWhenDeleteResourceByNegativeIds(@Server(service = RESOURCE) MockServer resourceServiceServer,
      @Server(service = SONG) MockServer songServiceServer) {

    resourceServiceServer.responseWithJson(HttpStatus.OK, Collections.emptyList(),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    songServiceServer.responseWithJson(HttpStatus.OK, Collections.emptyList(),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/resources")
            .queryParam("id", "-1,-3")
            .build())
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void shouldThrowDeleteFailedExceptionWhenDeleteResourceByIds(@Server(service = RESOURCE) MockServer resourceServiceServer) {
    resourceServiceServer.responseWithJson(HttpStatus.INTERNAL_SERVER_ERROR, new APIError(HttpStatus.INTERNAL_SERVER_ERROR,
            "Resource file deletion has failed", new RuntimeException()),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/resources")
            .queryParam("id", "1,2")
            .build())
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isEqualTo("INTERNAL_SERVER_ERROR")
        .jsonPath("$.message").value(containsString("Resource file deletion has failed"));
  }

  private Buffer fileBuffer() throws IOException {
    File file = testFile();
    Buffer buffer = Okio.buffer(Okio.source(file)).getBuffer();
    Okio.use(buffer, (Function1<Buffer, Object>) buffer1 -> {
      try {
        return buffer.writeAll(Okio.source(file));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    return buffer;
  }

  private File testFile() throws IOException {
    File file = ResourceUtils.getFile("src/test/resources/files/mpthreetest.mp3");
    File testFile = ResourceUtils.getFile("src/test/resources/files/test.mp3");
    if (!testFile.exists()) {
      Files.copy(file.toPath(), testFile.toPath());
    }
    return testFile;
  }
}
