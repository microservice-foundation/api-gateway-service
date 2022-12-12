package com.epam.learn.microservices.apigatewayservices.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyResponseBodyGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PostDeleteSongGatewayFilterFactory extends
        AbstractGatewayFilterFactory<PostDeleteSongGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(PostDeleteSongGatewayFilterFactory.class);
    private final ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter;
    private final WebClient webClient;
    ParameterizedTypeReference<List<Map<String, Object>>> jsonType =
            new ParameterizedTypeReference<>() {
            };

    @Autowired
    public PostDeleteSongGatewayFilterFactory(ModifyResponseBodyGatewayFilterFactory modifyResponseBodyFilter,
                                              ReactorLoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction) {
        super(Config.class);
        this.modifyResponseBodyFilter = modifyResponseBodyFilter;
        this.webClient = WebClient.builder()
                .filter(loadBalancerExchangeFilterFunction)
                .build();
    }

    @Override
    public GatewayFilter apply(PostDeleteSongGatewayFilterFactory.Config config) {
        log.info("Post-deleting song metadata under config '{}'", config);
        return modifyResponseBodyFilter.apply((c) -> {
            c.setRewriteFunction(List.class, List.class, (filterExchange, input) -> {
                List<Map<String, Object>> castedInput = (List<Map<String, Object>>) input;
                String baseFieldValues = castedInput.stream()
                        .map(bodyMap -> bodyMap.get(config.originBaseField).toString())
                        .collect(Collectors.joining(","));

                return webClient
                        .delete()
                        .uri(UriComponentsBuilder.fromHttpUrl(config.targetBaseUrl)
                            .path(config.targetPath)
                            .queryParam(config.queryParam, baseFieldValues)
                            .build().toUri())
                    .exchangeToMono(clientResponse -> clientResponse.bodyToMono(jsonType)
                        .map(songRecordIdList -> {
                            Map<String, Object> songRecordByResourceId = new HashMap<>();
                            songRecordIdList.forEach(songRecordId -> songRecordByResourceId.put("resourceId",
                                    songRecordId.get("id")));

                            Map<Integer, Map<String, Object>> songRecordIdEntries = songRecordIdList.stream()
                                    .collect(Collectors.toMap(pr -> (int)pr.get("id"), pr->songRecordByResourceId));

                            return castedInput.stream().map(originEntry -> {
                                originEntry.put(config.composeField, songRecordIdEntries.get(
                                        originEntry.get(config.originBaseField)));

                                return originEntry;
                            }).collect(Collectors.toList());
                        })
                    );
            });
        });
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("originBaseField", "targetBaseUrl", "targetPath", "queryParam", "composeField");
    }

    public static class Config {
        private final String originBaseField;
        private final String targetBaseUrl;
        private final String targetPath;
        private final String queryParam;
        private final String composeField;

        public Config(String originBaseField, String targetBaseUrl, String targetPath, String queryParam, String composeField) {
            this.originBaseField = originBaseField;
            this.targetBaseUrl = targetBaseUrl;
            this.targetPath = targetPath;
            this.queryParam = queryParam;
            this.composeField = composeField;
        }
    }
}
