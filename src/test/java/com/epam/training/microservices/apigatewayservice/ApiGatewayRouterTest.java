package com.epam.training.microservices.apigatewayservice;

import static com.epam.training.microservices.apigatewayservice.common.Server.Service.RESOURCE;
import static com.epam.training.microservices.apigatewayservice.common.Server.Service.SONG;
import static com.epam.training.microservices.apigatewayservice.common.Server.Service.STORAGE;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import com.epam.training.microservices.apigatewayservice.common.MockServer;
import com.epam.training.microservices.apigatewayservice.common.MockServerExtension;
import com.epam.training.microservices.apigatewayservice.common.Server;
import com.epam.training.microservices.apigatewayservice.configuration.GatewayConfiguration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    mockServer.response(HttpStatus.BAD_REQUEST);

    webTestClient.get()
        .uri("/resources/{id}", 123L)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  void shouldReturnNotFoundWhenGetResourceById(@Server(service = RESOURCE) MockServer mockServer) {
    mockServer.response(HttpStatus.NOT_FOUND);

    webTestClient.get()
        .uri("/resources/{id}", 123L)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus().isNotFound();
  }

  @Test
  void shouldReturnNotFoundResourceFileWhenGetResourceById(@Server(service = RESOURCE) MockServer mockServer) {
    mockServer.response(HttpStatus.NOT_FOUND);

    webTestClient.get()
        .uri("/resources/{id}", 123L)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus().isNotFound();
  }

  @Test
  void shouldReturnDownloadFileErrorWhenGetResourceById(@Server(service = RESOURCE) MockServer mockServer) {
    mockServer.response(HttpStatus.INTERNAL_SERVER_ERROR);

    webTestClient.get()
        .uri("/resources/{id}", 123L)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus().is5xxServerError();
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
    mockServer.response(HttpStatus.BAD_REQUEST);

    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", testFile());

    webTestClient.post().uri("/resources")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  void shouldThrowValidationExceptionWhenSaveResourceWithEmptyFile(@Server(service = RESOURCE) MockServer mockServer) {
    mockServer.response(HttpStatus.BAD_REQUEST);

    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", new byte[0]);

    webTestClient.post().uri("/resources")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  void shouldThrowValidationExceptionWhenSaveInvalidTypeResource(@Server(service = RESOURCE) MockServer mockServer) {
    mockServer.response(HttpStatus.BAD_REQUEST);

    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("file", "this is string");

    webTestClient.post().uri("/resources")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts))
        .exchange()
        .expectStatus().isBadRequest();
  }


  @Test
  void shouldThrowUploadFailedExceptionWhenSaveResource(@Server(service = RESOURCE) MockServer mockServer) throws IOException {
    mockServer.response(HttpStatus.INTERNAL_SERVER_ERROR);

    MultiValueMap<String, Object> parts2 = new LinkedMultiValueMap<>();
    parts2.add("file", new FileSystemResource(testFile()));

    webTestClient.post().uri("/resources")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(parts2))
        .exchange()
        .expectStatus().is5xxServerError();
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
  void shouldReturnEmptyWhenResourceByIds(@Server(service = RESOURCE) MockServer resourceServiceServer,
      @Server(service = SONG) MockServer songServiceResource) {
    List<Map<String, Object>> resourceRecords = new ArrayList<>();
    resourceServiceServer.responseWithJson(HttpStatus.OK, resourceRecords,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    songServiceResource.response(HttpStatus.BAD_REQUEST);

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/resources")
            .queryParam("id", "1")
            .build())
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  void shouldThrowExceptionWhenDeleteResourceByEmptyIdsList(@Server(service = RESOURCE) MockServer resourceServiceServer) {
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", new Date());
    error.put("path", "/api/v1/resources");
    error.put("status", "400");
    error.put("message", "Invalid params");
    resourceServiceServer.responseWithJson(HttpStatus.BAD_REQUEST, error,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/resources")
            .queryParam("id", "")
            .build())
        .exchange()
        .expectStatus()
        .isBadRequest();
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
    resourceServiceServer.response(HttpStatus.INTERNAL_SERVER_ERROR);

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/resources")
            .queryParam("id", "1,2")
            .build())
        .exchange()
        .expectStatus().is5xxServerError();
  }

  @Test
  void shouldSaveSongRecord(@Server(service = SONG) MockServer songServiceServer) {
    long id = 1233L;
    songServiceServer.responseWithJson(HttpStatus.CREATED, Collections.singletonMap("id", id),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.post().uri("/songs")
        .bodyValue(buildSongMetadata())
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.id").isEqualTo(id);
  }

  @Test
  void shouldThrowValidationExceptionWhenSaveSongWithInvalidResourceId(@Server(service = SONG) MockServer songServiceServer) {
    songServiceServer.response(HttpStatus.BAD_REQUEST);

    webTestClient.post().uri("/songs")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(buildInvalidSongMetadata())
        .exchange()
        .expectStatus().isBadRequest();
  }

  private Map<String, Object> buildSongMetadata() {
    Map<String, Object> map = new HashMap<>();
    map.put("resourceId", 123);
    map.put("name", "Hello World");
    map.put("length", "54:32");
    map.put("album", "Tech");
    map.put("artist", "artist");
    map.put("year", 2009);

    return map;
  }

  @Test
  void shouldGetSongMetadata(@Server(service = SONG) MockServer songServiceServer) {
    Map<String, Object> songMetadata = buildSongMetadata();
    songMetadata.put("id", 123L);
    songServiceServer.responseWithJson(HttpStatus.OK, songMetadata,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.get().uri("/songs/{id}", 123L)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id").isEqualTo(songMetadata.get("id"))
        .jsonPath("$.resourceId").isEqualTo(songMetadata.get("resourceId"))
        .jsonPath("$.name").isEqualTo(songMetadata.get("name"))
        .jsonPath("$.length").isEqualTo(songMetadata.get("length"))
        .jsonPath("$.album").isEqualTo(songMetadata.get("album"))
        .jsonPath("$.artist").isEqualTo(songMetadata.get("artist"));
  }
  private Map<String, Object> buildInvalidSongMetadata() {
    return Map.of(
        "resourceId", -123L,
        "name", "Hello World",
        "length", "54:32",
        "album", "Tech",
        "artist", "artist",
        "year", 2090
    );
  }

  @Test
  void shouldThrowExceptionWhenGetSongMetadata(@Server(service = SONG) MockServer songServiceServer) {
    songServiceServer.response(HttpStatus.NOT_FOUND);

    webTestClient.get().uri("/songs/{id}", 124567L)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void shouldDeleteSongMetadataById(@Server(service = SONG) MockServer songServiceServer) {
    long[] songMetadataIds = {100L, 101L};
    List<Map<String, Object>> songRecords = new ArrayList<>();
    long resourceId1 = 1L;
    songRecords.add(Map.of("id", songMetadataIds[0], "resourceId", resourceId1));
    long resourceId2 = 2L;
    songRecords.add(Map.of("id", songMetadataIds[1], "resourceId", resourceId2));
    songServiceServer.responseWithJson(HttpStatus.OK, songRecords,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/songs")
            .queryParam("id", songMetadataIds[0] + "," + songMetadataIds[1])
            .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$[*].id").value(containsInAnyOrder(
            is((int) songMetadataIds[0]),
            is((int) songMetadataIds[1])))
        .jsonPath("$[*].resourceId").value(containsInAnyOrder(
            is((int) resourceId1),
            is((int) resourceId2)));
  }

  @Test
  void shouldReturnEmptyWhenDeleteSongMetadataByNegativeResourceIds(@Server(service = SONG) MockServer songServiceServer) {
    songServiceServer.responseWithJson(HttpStatus.OK, Collections.emptyList(),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.delete().uri(uriBuilder -> uriBuilder.path("/songs/by-resource-id").queryParam("id", "-1,-3").build())
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void shouldReturnEmptyWhenDeleteSongMetadataByNegativeIds(@Server(service = SONG) MockServer songServiceServer) {
    songServiceServer.responseWithJson(HttpStatus.OK, Collections.emptyList(),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    webTestClient.delete().uri(uriBuilder -> uriBuilder.path("/songs").queryParam("id", "-1,-3").build())
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void shouldThrowErrorWhenDeleteSongMetadataByEmptyId(@Server(service = SONG) MockServer songServiceServer) {
    songServiceServer.response(HttpStatus.BAD_REQUEST);

    webTestClient.delete().uri(uriBuilder -> uriBuilder.path("/songs").queryParam("id", "").build())
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void shouldDeleteSongMetadataByResourceId(@Server(service = SONG) MockServer songServiceServer) {
    long[] songMetadataIds = {100L, 101L};
    List<Map<String, Object>> songRecords = new ArrayList<>();
    long resourceId1 = 1L;
    songRecords.add(Map.of("id", songMetadataIds[0], "resourceId", resourceId1));
    long resourceId2 = 2L;
    songRecords.add(Map.of("id", songMetadataIds[1], "resourceId", resourceId2));
    songServiceServer.responseWithJson(HttpStatus.OK, songRecords,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.delete().uri(uriBuilder -> uriBuilder
            .path("/songs/by-resource-id")
            .queryParam("id", resourceId1 + "," + resourceId2)
            .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$[*].id").value(containsInAnyOrder(
            is((int) songMetadataIds[0]),
            is((int) songMetadataIds[1])))
        .jsonPath("$[*].resourceId").value(containsInAnyOrder(
            is((int) resourceId1),
            is((int) resourceId2)));
  }

  @Test
  void shouldThrowErrorWhenDeleteSongMetadataByEmptyResourceId(@Server(service = SONG) MockServer songServiceServer) {
    songServiceServer.response(HttpStatus.BAD_REQUEST);

    webTestClient.delete().uri(uriBuilder -> uriBuilder.path("/songs/by-resource-id").queryParam("id", "").build())
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void shouldGetAllStagingStoragesByType(@Server(service = STORAGE) MockServer storageServiceServer) {
    Map<String, Object> storage1 = buildStorage(StorageType.STAGING);
    Map<String, Object> storage2 = buildStorage(StorageType.STAGING);
    storageServiceServer.responseWithJson(HttpStatus.OK, List.of(storage1, storage2),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.get().uri(uriBuilder -> uriBuilder
        .path("/storages")
        .queryParam("type", StorageType.STAGING)
        .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[*].id").value(containsInAnyOrder(storage1.get("id"), storage2.get("id")))
        .jsonPath("$[*].bucket").value(containsInAnyOrder(storage1.get("bucket"), storage2.get("bucket")))
        .jsonPath("$[*].type").value(containsInAnyOrder(storage1.get("type"), storage2.get("type")))
        .jsonPath("$[*].path").value(containsInAnyOrder(storage1.get("path"), storage2.get("path")));

    Map<String, Object> storage3 = buildStorage(StorageType.PERMANENT);
    Map<String, Object> storage4 = buildStorage(StorageType.PERMANENT);
    storageServiceServer.responseWithJson(HttpStatus.OK, List.of(storage3, storage4),
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.get().uri(uriBuilder -> uriBuilder
            .path("/storages")
            .queryParam("type", StorageType.PERMANENT)
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[*].id").value(containsInAnyOrder(storage3.get("id"), storage4.get("id")))
        .jsonPath("$[*].bucket").value(containsInAnyOrder(storage3.get("bucket"), storage4.get("bucket")))
        .jsonPath("$[*].type").value(containsInAnyOrder(storage3.get("type"), storage4.get("type")))
        .jsonPath("$[*].path").value(containsInAnyOrder(storage3.get("path"), storage4.get("path")));
  }

  @Test
  void shouldThrowNotFoundExceptionWhenGetStoragesByType(@Server(service = STORAGE) MockServer storageServiceServer) {
    StorageType stagingType = StorageType.STAGING;
    storageServiceServer.response(HttpStatus.NOT_FOUND);

    webTestClient.get().uri(uriBuilder -> uriBuilder
            .path("/storages")
            .queryParam("type", StorageType.PERMANENT)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound();

    StorageType permanentType = StorageType.PERMANENT;
    storageServiceServer.response(HttpStatus.NOT_FOUND);

    webTestClient.get().uri(uriBuilder -> uriBuilder
            .path("/storages")
            .queryParam("type", StorageType.PERMANENT)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void shouldGetStorageById(@Server(service = STORAGE) MockServer storageServiceServer) {
    Map<String, Object> storage1 = buildStorage(StorageType.STAGING);
    storageServiceServer.responseWithJson(HttpStatus.OK, storage1,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.get().uri("/storages/{id}", storage1.get("id"))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id").isEqualTo(storage1.get("id"))
        .jsonPath("$.bucket").isEqualTo(storage1.get("bucket"))
        .jsonPath("$.path").isEqualTo(storage1.get("path"))
        .jsonPath("$.type").isEqualTo(storage1.get("type"));

    Map<String, Object> storage2 = buildStorage(StorageType.PERMANENT);
    storageServiceServer.responseWithJson(HttpStatus.OK, storage2,
        Collections.singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    webTestClient.get().uri("/storages/{id}", storage2.get("id"))
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id").isEqualTo(storage2.get("id"))
        .jsonPath("$.bucket").isEqualTo(storage2.get("bucket"))
        .jsonPath("$.path").isEqualTo(storage2.get("path"))
        .jsonPath("$.type").isEqualTo(storage2.get("type"));
  }

  @Test
  void shouldThrowNotFoundExceptionWhenGetStorageById(@Server(service = STORAGE) MockServer storageServiceServer) {
    long id1 = 123_234_533L;
    storageServiceServer.response(HttpStatus.NOT_FOUND);

    webTestClient.get().uri("/storages/{id}", id1)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound();

    long id2 = 123_234_512L;
    storageServiceServer.response(HttpStatus.NOT_FOUND);

    webTestClient.get().uri("/storages/{id}", id2)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  private Map<String, Object> buildStorage(StorageType type) {
    int id = new Random().nextInt(1000);
    return Map.of(
        "id", id,
        "bucket", type.name().toLowerCase() + "-bucket" + id,
        "path", "files/",
        "type", type.name()
    );
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

  private enum StorageType {
    PERMANENT,
    STAGING;
  }
}
