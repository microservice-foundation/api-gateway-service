package com.epam.training.microservices.apigatewayservice.contract.songservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@ExtendWith(SpringExtension.class)
@AutoConfigureStubRunner(stubsMode = StubRunnerProperties.StubsMode.LOCAL,
        ids = "com.epam.training:song-service:+:stubs:1001")
@TestPropertySource(locations = "classpath:application.properties")
@DirtiesContext
class SongServiceControllerTest {
    private WebTestClient webClient;
    @BeforeEach
    public void setup() {
        webClient = WebTestClient
                        .bindToServer(new ReactorClientHttpConnector())
                        .baseUrl("http://localhost:1001/api/v1/songs")
                        .build();
    }

    @Test
    void shouldDeleteSongMetadataByResourceId() {
        webClient.delete()
            .uri(uriBuilder -> uriBuilder.path("/delete-by-resource-id").queryParam("id", 1L).build())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.[0].id").isEqualTo(199L)
            .jsonPath("$.[0].resourceId").isEqualTo(1L);
    }

    @Test
    void shouldDeleteSongMetadataById() {
        webClient.delete()
            .uri(uriBuilder -> uriBuilder.queryParam("id", 199L).build())
            .header(HttpHeaders.ACCEPT, "application/json")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.[0].id").isEqualTo(199L)
            .jsonPath("$.[0].resourceId").isEqualTo(1L);
    }

    @Test
    void shouldGetSongMetadataById() {
        webClient.get()
            .uri(uriBuilder -> uriBuilder.path("/{id}").build(199L))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.resourceId").isEqualTo(1L)
            .jsonPath("$.id").isEqualTo(199L)
            .jsonPath("$.name").isEqualTo("Saturday")
            .jsonPath("$.artist").isEqualTo("John Biden")
            .jsonPath("$.album").isEqualTo("2023")
            .jsonPath("$.length").isEqualTo("03:40")
            .jsonPath("$.year").isEqualTo("1990");
    }

    @Test
    void shouldReturnNotFoundWhenGetNonexistentSongMetadataById() {
        webClient.get()
            .uri(uriBuilder -> uriBuilder.path("/{id}").build(1999L))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.status").isEqualTo("NOT_FOUND")
            .jsonPath("$.message").isEqualTo("Song metadata not found")
            .jsonPath("$.debugMessage").isEqualTo("Song was not found with id '1999'");
    }

    @Test
    void shouldReturnBadRequestWhenDeleteSongMetadataById() {
        webClient.delete()
            .uri(uriBuilder -> uriBuilder.queryParam("id", "").build())
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.status").isEqualTo("BAD_REQUEST")
            .jsonPath("$.message").isEqualTo("Invalid request")
            .jsonPath("$.debugMessage").isEqualTo("Id param was not validated, check your ids");
    }

    @Test
    void shouldReturnBadRequestWhenDeleteSongMetadataByResourceId() {
        webClient.delete()
            .uri(uriBuilder -> uriBuilder.path("/delete-by-resource-id").queryParam("id", "").build())
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.status").isEqualTo("BAD_REQUEST")
            .jsonPath("$.message").isEqualTo("Invalid request")
            .jsonPath("$.debugMessage").isEqualTo("Id param was not validated, check your ids");
    }

    @Test
    void shouldSaveSongMetadata() {
        String body = "{\"resourceId\":\"1\",\"name\":\"New office\",\"artist\":\"John Kennedy\",\"album\":\"ASU\"," +
                "\"length\":\"03:22\",\"year\":\"1999\"}";
        webClient.post()
            .body(BodyInserters.fromValue(body))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.id").isEqualTo(199L)
            .jsonPath("$.resourceId").isEqualTo(1L);
    }

    @Test
    void shouldReturnBadRequestWhenSaveSongMetadata() {
        String body = "{\"resourceId\":\"1\",\"name\":\"New office\",\"artist\":\"John Kennedy\",\"album\":\"ASU\"," +
                "\"length\":\"03:22\",\"year\":\"2099\"}";
        webClient.post()
            .body(BodyInserters.fromValue(body))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.status").isEqualTo("BAD_REQUEST")
            .jsonPath("$.message").isEqualTo("Invalid request")
            .jsonPath("$.debugMessage").isEqualTo("Saving invalid song record");
    }
}
