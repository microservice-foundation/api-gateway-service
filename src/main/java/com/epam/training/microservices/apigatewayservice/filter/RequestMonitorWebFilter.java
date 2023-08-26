package com.epam.training.microservices.apigatewayservice.filter;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.context.ContextSnapshot;
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

//         !! IMPORTANT STEP !!
//         Preparing context for the Tracer Span used in TracerConfiguration
        .contextWrite(context -> {
          ContextSnapshot.setThreadLocalsFrom(context, ObservationThreadLocalAccessor.KEY);
          return context;
        })

//         Logging the metrics for the API call, not really required to have this section for tracing setup
        .doFinally(signalType -> {
          long endTime = System.currentTimeMillis();
          long executionTime = endTime - startTime;


//          Extracting traceId added in TracerConfiguration Webfilter bean
          final List<String> traceIds = ofNullable(exchange.getResponse().getHeaders().get("traceId")).orElseGet(List::of);
          final MetricsLogTemplate metricsLogTemplate = new MetricsLogTemplate(
              String.join(",", traceIds),
              exchange.getLogPrefix().trim(),
              executionTime);
          try {
            log.info(new ObjectMapper().writeValueAsString(metricsLogTemplate));
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private static final class MetricsLogTemplate {
    private final String traceId;
    private final String logPrefix;
    private final long executionTime;

    public MetricsLogTemplate(String traceId, String logPrefix, long executionTime) {
      this.traceId = traceId;
      this.logPrefix = logPrefix;
      this.executionTime = executionTime;
    }

    public String getTraceId() {
      return traceId;
    }

    public String getLogPrefix() {
      return logPrefix;
    }

    public long getExecutionTime() {
      return executionTime;
    }
  }
}
