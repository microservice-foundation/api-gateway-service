package com.epam.training.microservices.apigatewayservice.contract.resourceservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@AutoConfigureStubRunner(stubsMode = StubRunnerProperties.StubsMode.LOCAL,
        ids = "com.epam.training:resource-service:+:stubs:1002")
@TestPropertySource(locations = "classpath:application.properties")
@DirtiesContext
class ResourceServiceControllerTest {
    private WebTestClient webClient;
    @BeforeEach
    public void setup() {
        webClient = WebTestClient
                .bindToServer(new ReactorClientHttpConnector())
                .baseUrl("http://localhost:1002/api/v1/resources")
                .build();
    }

    @Test
    void shouldDeleteResources() {
        webClient.delete()
            .uri(uriBuilder -> uriBuilder.queryParam("id", 1L).build())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id", 1L);
    }

    @Test
    void shouldReturnBadRequestWhenDeleteResources() {
        webClient.delete()
            .uri(uriBuilder -> uriBuilder.queryParam("id", "").build())
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.status").isEqualTo("BAD_REQUEST")
            .jsonPath("$.message").isEqualTo("Invalid request")
            .jsonPath("$.debugMessage").isEqualTo("Id param was not validated, check your file");
    }

    @Test
    void shouldGetResourceById() {
        EntityExchangeResult<byte[]> entityExchangeResult = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/{id}").build(123L))
                .accept(MediaType.valueOf("audio/mpeg"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult();

        assertNotNull(entityExchangeResult.getResponseBody());
    }

    @Test
    void shouldReturnNotFoundWhenGetNonexistentResourceById() {
        webClient.get()
            .uri(uriBuilder -> uriBuilder.path("/{id}").build(1999L))
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.status").isEqualTo( "NOT_FOUND")
            .jsonPath("$.message").isEqualTo("Resource not found")
            .jsonPath("$.debugMessage").isEqualTo("Resource with id=1999 not found");

    }

//TODO: uncomment when switch to reactive

    @Test
    void shouldSaveResource() {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("multipartFile", new byte[]{12, 43, 21, 12, 55}, MediaType.valueOf("audio/mpeg")).header(
                "Content-Disposition", "form-data;");

        webClient.post()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1L);
    }
}