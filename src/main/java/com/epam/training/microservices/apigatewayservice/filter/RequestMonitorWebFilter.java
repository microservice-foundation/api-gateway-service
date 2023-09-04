package com.epam.training.microservices.apigatewayservice.filter;

import static java.util.Optional.ofNullable;

import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class RequestMonitorWebFilter implements WebFilter {
  private final Logger log = LoggerFactory.getLogger(RequestMonitorWebFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    final long startTime = System.currentTimeMillis();
    return chain.filter(exchange)
        .contextWrite(context -> {
          ContextSnapshotFactory.builder().build().setThreadLocalsFrom(context, ObservationThreadLocalAccessor.KEY);
          return context;
        })
        .doFinally(signalType -> {
          final long endTime = System.currentTimeMillis();
          final long executionTime = endTime - startTime;
          final List<String> traceIds = ofNullable(exchange.getResponse().getHeaders().get("traceId")).orElseGet(List::of);
          log.info("Request completed, execution time: {}, log prefix: {}, trace id: {}.", executionTime, exchange.getLogPrefix(), traceIds);
        });
  }
}
