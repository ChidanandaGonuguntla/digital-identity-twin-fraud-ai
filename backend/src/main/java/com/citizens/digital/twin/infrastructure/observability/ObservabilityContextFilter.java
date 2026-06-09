package com.citizens.digital.twin.infrastructure.observability;

import com.citizens.digital.twin.shared.observability.ObservabilityContext;
import com.citizens.digital.twin.shared.observability.TechnicalHeaders;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ObservabilityContextFilter extends OncePerRequestFilter {
  private final Optional<Tracer> tracer;

  public ObservabilityContextFilter(Optional<Tracer> tracer) {
    this.tracer = tracer;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = headerOrNew(request, TechnicalHeaders.CORRELATION_ID);
    String traceId =
        tracer
            .flatMap(t -> Optional.ofNullable(t.currentSpan()))
            .flatMap(span -> Optional.ofNullable(span.context()))
            .map(ctx -> ctx.traceId())
            .filter(id -> !id.isBlank())
            .orElse(headerOrNew(request, TechnicalHeaders.TRACE_ID));
    MDC.put(ObservabilityContext.MDC_CORRELATION_ID, correlationId);
    MDC.put(ObservabilityContext.MDC_TRACE_ID, traceId);
    putIfPresent(request, TechnicalHeaders.SOURCE_SYSTEM, ObservabilityContext.MDC_SOURCE_SYSTEM);
    putIfPresent(request, TechnicalHeaders.EVENT_ID, ObservabilityContext.MDC_EVENT_ID);
    putIfPresent(
        request, TechnicalHeaders.EVENT_TIMESTAMP, ObservabilityContext.MDC_EVENT_TIMESTAMP);
    response.setHeader(TechnicalHeaders.CORRELATION_ID, correlationId);
    response.setHeader(TechnicalHeaders.TRACE_ID, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  private void putIfPresent(HttpServletRequest request, String header, String mdcKey) {
    String value = request.getHeader(header);
    if (value != null && !value.isBlank()) {
      MDC.put(mdcKey, value);
    }
  }

  private String headerOrNew(HttpServletRequest request, String header) {
    String value = request.getHeader(header);
    return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
  }
}
