package com.epam.training.microservices.apigatewayservice.router;

import com.epam.training.microservices.apigatewayservice.configuration.RateLimiterProperties;
import com.epam.training.microservices.apigatewayservice.configuration.ResourceServiceProperties;
import com.epam.training.microservices.apigatewayservice.configuration.SongServiceProperties;
import com.epam.training.microservices.apigatewayservice.filter.DeleteSongMetadataGatewayFilterFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(value = {ResourceServiceProperties.class, SongServiceProperties.class, RateLimiterProperties.class})
public class Router {

  //TODO: security and key resolver should be implemented
  private static final String PATH_ID = "/{id}";
  private static final String PATH_RESOURCES = "/resources";
  private static final String PATH_SONGS = "/songs";
  private static final String PATH_DELETE_SONG_METADATA_BY_RESOURCE_ID = "/delete-by-resource-id";
  private static final String QUERY_PARAM_DELETE_SONG_METADATA = "id";
  private static final String QUERY_PARAM_DELETE_RESOURCE = "id";

  @Value("${spring.application.name}")
  private String applicationName;

  @Bean
  public RouteLocator routes(RouteLocatorBuilder builder, ResourceServiceProperties resourceServiceProperties,
      SongServiceProperties songServiceProperties, RateLimiterProperties rateLimiterProperties,
      DeleteSongMetadataGatewayFilterFactory deleteSongMetadataGatewayFilterFactory) {

    return builder.routes()
        .route("get-resources", route -> route
            .method(HttpMethod.GET).and()
            .path(PATH_RESOURCES + PATH_ID)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class,
                    config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate()).setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                        .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(resourceServiceProperties.getPath()))
            .uri(resourceServiceProperties.getUri())
        )
        .route("post-resources", route -> route
            .method(HttpMethod.POST).and()
            .path(PATH_RESOURCES)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class,
                    config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate()).setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                        .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(resourceServiceProperties.getPath()))
            .uri(resourceServiceProperties.getUri())
        )
        .route("delete-resources", route -> route
            .method(HttpMethod.DELETE).and()
            .path(PATH_RESOURCES).and()
            .query(QUERY_PARAM_DELETE_RESOURCE)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class,
                    config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate()).setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                        .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(resourceServiceProperties.getPath())
                .filter(deleteSongMetadataGatewayFilterFactory.apply(new DeleteSongMetadataGatewayFilterFactory.Config(
                    PATH_SONGS + PATH_DELETE_SONG_METADATA_BY_RESOURCE_ID, QUERY_PARAM_DELETE_SONG_METADATA))
                )
            )
            .uri(resourceServiceProperties.getUri())
        )
        .route("get-songs", route -> route
            .method(HttpMethod.GET).and()
            .path(PATH_SONGS + PATH_ID)
            .filters(filter -> filter.setPath(songServiceProperties.getPath())
                .requestRateLimiter(config -> config.setRateLimiter(redisRateLimiter(rateLimiterProperties))))
            .uri(songServiceProperties.getUri())
        )
        .route("post-songs", route -> route
            .method(HttpMethod.POST).and()
            .path(PATH_SONGS)
            .filters(filter -> filter.setPath(songServiceProperties.getPath())
                .requestRateLimiter(config -> config.setRateLimiter(redisRateLimiter(rateLimiterProperties))))
            .uri(songServiceProperties.getUri())
        )
        .route("delete-songs-by-resource-id", route -> route
            .method(HttpMethod.DELETE).and()
            .path(PATH_SONGS + PATH_DELETE_SONG_METADATA_BY_RESOURCE_ID).and()
            .query(QUERY_PARAM_DELETE_SONG_METADATA)
            .filters(filter -> filter
                .requestRateLimiter().rateLimiter(RedisRateLimiter.class, config -> config.setReplenishRate(rateLimiterProperties.getReplenishRate()).setBurstCapacity(rateLimiterProperties.getBurstCapacity())
                    .setRequestedTokens(rateLimiterProperties.getRequestedTokens())).and()
                .setPath(songServiceProperties.getPath()))
            .uri(songServiceProperties.getUri())
        ).build();
  }

  private RedisRateLimiter redisRateLimiter(RateLimiterProperties properties) {
    return new RedisRateLimiter(properties.getReplenishRate(), properties.getBurstCapacity(), properties.getRequestedTokens());
  }
}