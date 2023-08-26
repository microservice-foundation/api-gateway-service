package com.epam.training.microservices.apigatewayservice.router;

import com.epam.training.microservices.apigatewayservice.configuration.RateLimiterProperties;
import com.epam.training.microservices.apigatewayservice.configuration.ResourceServiceProperties;
import com.epam.training.microservices.apigatewayservice.configuration.SongServiceProperties;
import com.epam.training.microservices.apigatewayservice.configuration.StorageServiceProperties;
import com.epam.training.microservices.apigatewayservice.filter.DeleteSongMetadataGatewayFilterFactory;
import io.micrometer.observation.annotation.Observed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(value = {ResourceServiceProperties.class, SongServiceProperties.class, StorageServiceProperties.class,
    RateLimiterProperties.class})
public class Router {

  //TODO: security and key resolver should be implemented
  private static final String PATH_ID = "/{id}";
  private static final String PATH_RESOURCES = "/resources";
  private static final String PATH_SONGS = "/songs";
  private static final String PATH_STORAGES = "/storages";
  private static final String PATH_DELETE_SONG_METADATA_BY_RESOURCE_ID = "/by-resource-id";
  private static final String QUERY_PARAM_ID = "id";
  private static final String QUERY_PARAM_TYPE = "type";

  @Value("${spring.application.name}")
  private String applicationName;

  @Bean
  public RouteLocator routes(RouteLocatorBuilder builder, ResourceServiceProperties resourceServiceProperties,
      SongServiceProperties songServiceProperties, StorageServiceProperties storageServiceProperties,
      RateLimiterProperties rateLimiterProperties, DeleteSongMetadataGatewayFilterFactory deleteSongMetadataGatewayFilterFactory) {

    return builder.routes()
        .route("get-resource-by-id", route -> route
            .method(HttpMethod.GET).and()
            .path(PATH_RESOURCES + PATH_ID)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class,
                    config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate())
                        .setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                        .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(resourceServiceProperties.getPath() + PATH_ID))
            .uri(resourceServiceProperties.getUri())
        )
        .route("post-resources", route -> route
            .method(HttpMethod.POST).and()
            .path(PATH_RESOURCES)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class,
                    config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate())
                        .setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                        .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(resourceServiceProperties.getPath()))
            .uri(resourceServiceProperties.getUri())
        )
        .route("delete-resources", route -> route
            .method(HttpMethod.DELETE).and()
            .path(PATH_RESOURCES).and()
            .query(QUERY_PARAM_ID)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class,
                    config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate())
                        .setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                        .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(resourceServiceProperties.getPath())
                .filter(deleteSongMetadataGatewayFilterFactory.apply(new DeleteSongMetadataGatewayFilterFactory.Config(
                    PATH_SONGS + PATH_DELETE_SONG_METADATA_BY_RESOURCE_ID, QUERY_PARAM_ID))
                )
            )
            .uri(resourceServiceProperties.getUri())
        )
        .route("get-song-by-id", route -> route
            .method(HttpMethod.GET).and()
            .path(PATH_SONGS + PATH_ID)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class,
                    config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate())
                        .setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                        .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(songServiceProperties.getPath() + PATH_ID))
            .uri(songServiceProperties.getUri())
        )
        .route("post-song", route -> route
            .method(HttpMethod.POST).and()
            .path(PATH_SONGS)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class,
                    config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate())
                        .setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                        .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(songServiceProperties.getPath()))
            .uri(songServiceProperties.getUri())
        )
        .route("delete-songs", route -> route
            .method(HttpMethod.DELETE).and()
            .path(PATH_SONGS).and()
            .query(QUERY_PARAM_ID)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class,
                    config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate())
                        .setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                        .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(songServiceProperties.getPath()))
            .uri(songServiceProperties.getUri())
        )
        .route("delete-songs-by-resource-id", route -> route
            .method(HttpMethod.DELETE).and()
            .path(PATH_SONGS + PATH_DELETE_SONG_METADATA_BY_RESOURCE_ID).and()
            .query(QUERY_PARAM_ID)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class,
                    config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate())
                        .setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                        .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(songServiceProperties.getPath() + songServiceProperties.getByResourceId()))
            .uri(songServiceProperties.getUri())
        )
        .route("get-storages-by-type", route -> route
            .method(HttpMethod.GET).and()
            .path(PATH_STORAGES).and()
            .query(QUERY_PARAM_TYPE)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class,
                    config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate())
                        .setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                        .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(storageServiceProperties.getPath()))
            .uri(storageServiceProperties.getUri()))
        .route("get-storage-by-id", route -> route
            .method(HttpMethod.GET).and()
            .path(PATH_STORAGES + PATH_ID)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class,
                    config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate())
                        .setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                        .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(storageServiceProperties.getPath() + PATH_ID))
            .uri(storageServiceProperties.getUri())
        )
        .build();
  }
}
