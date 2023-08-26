package com.epam.training.microservices.apigatewayservice.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class DeleteSongMetadataGatewayFilterFactory extends AbstractGatewayFilterFactory<DeleteSongMetadataGatewayFilterFactory.Config> {
  private static final Logger log = LoggerFactory.getLogger(DeleteSongMetadataGatewayFilterFactory.class);
  private static final String FIELD_RESOURCE_ID = "resourceId";
  private static final String FIELD_ID = "id";
  private static final String FIELD_SONG_METADATA = "song_metadata";
  private final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory;
  private final WebClient webClient;


  @Autowired
  public DeleteSongMetadataGatewayFilterFactory(ModifyResponseBodyGatewayFilterFactory modifyResponseBodyGatewayFilterFactory,
      WebClient webClient) {
    super(Config.class);
    this.modifyResponseBodyGatewayFilterFactory = modifyResponseBodyGatewayFilterFactory;
    this.webClient = webClient;
  }

  @Override
  public GatewayFilter apply(DeleteSongMetadataGatewayFilterFactory.Config config) {
    return modifyResponseBodyGatewayFilterFactory.apply(c -> c.setRewriteFunction(Object.class, Object.class,
        (serverWebExchange, response) -> {
          HttpStatusCode statusCode = serverWebExchange.getResponse().getStatusCode();
          log.info("Processing response body with status code {}", statusCode);
          if (statusCode != null && statusCode.isError()) {
            return Mono.just(response);
          }
          List<Map<String, Object>> resourceRecords = (List<Map<String, Object>>) response;
          String resourceIds = extractResourceIds(resourceRecords);
          log.debug("Deleted resource ids {}", resourceIds);
          return deleteSongMetadataByResourceIds(config, resourceIds, resourceRecords);
        }));
  }

  private Mono<Object> deleteSongMetadataByResourceIds(Config config, String resourceIds, List<Map<String, Object>> resourceRecords) {
    log.info("Sending a request to delete song records by resource ids '{}'", resourceIds);
    return webClient.delete()
        .uri(uriBuilder -> uriBuilder
            .path(config.resourcePath)
            .queryParam(config.queryParam, resourceIds)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchangeToMono(clientResponse -> clientResponse.bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
            .map(songRecords -> mapSongRecordToResourceRecord(songRecords, resourceRecords)));
  }

  private List<Map<String, Object>> mapSongRecordToResourceRecord(List<Map<String, Object>> songRecords,
      List<Map<String, Object>> resourceRecords) {
    log.info("Deleted song records: {}", songRecords);
    Map<Integer, Map<String, Object>> songRecordEntries = songRecords.stream()
        .collect(Collectors.toMap(pr -> (int) pr.get(FIELD_RESOURCE_ID), pr -> pr, (a, b) -> a, () -> new HashMap<>(songRecords.size())));

    resourceRecords.parallelStream().forEach(resourceRecord -> populateSongRecord.apply(resourceRecord, songRecordEntries));
    log.debug("Mapped resource records after deleting song records: {}", resourceRecords);
    return resourceRecords;
  }

  BiFunction<Map<String, Object>, Map<Integer, Map<String, Object>>, Map<String, Object>> populateSongRecord =
      (resourceRecord, songRecordsByResourceId) -> {

        resourceRecord.put(FIELD_SONG_METADATA, songRecordsByResourceId.get(resourceRecord.get(FIELD_ID)));
        return resourceRecord;
      };

  private String extractResourceIds(List<Map<String, Object>> input) {
    return input.stream()
        .map(bodyMap -> bodyMap.get(FIELD_ID).toString())
        .collect(Collectors.joining(","));
  }

  public static class Config {
    private final String resourcePath;
    private final String queryParam;

    public Config(String resourcePath, String queryParam) {
      this.resourcePath = resourcePath;
      this.queryParam = queryParam;
    }
  }
}
