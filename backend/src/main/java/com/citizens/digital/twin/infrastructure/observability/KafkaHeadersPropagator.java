package com.citizens.digital.twin.infrastructure.observability;

import com.citizens.digital.twin.shared.observability.ObservabilityContext;
import com.citizens.digital.twin.shared.observability.TechnicalHeaders;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

public final class KafkaHeadersPropagator {
  private KafkaHeadersPropagator() {}

  public static void apply(Headers headers) {
    String correlationId = header(headers, TechnicalHeaders.CORRELATION_ID);
    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
    }
    MDC.put(ObservabilityContext.MDC_CORRELATION_ID, correlationId);
    putMdc(headers, TechnicalHeaders.TRACE_ID, ObservabilityContext.MDC_TRACE_ID, correlationId);
    putMdc(headers, TechnicalHeaders.SOURCE_SYSTEM, ObservabilityContext.MDC_SOURCE_SYSTEM, null);
    putMdc(headers, TechnicalHeaders.EVENT_ID, ObservabilityContext.MDC_EVENT_ID, null);
    putMdc(
        headers, TechnicalHeaders.EVENT_TIMESTAMP, ObservabilityContext.MDC_EVENT_TIMESTAMP, null);
  }

  public static void clear() {
    MDC.clear();
  }

  private static void putMdc(Headers headers, String header, String mdcKey, String fallback) {
    String value = header(headers, header);
    if (value == null) {
      value = fallback;
    }
    if (value != null) {
      MDC.put(mdcKey, value);
    }
  }

  private static String header(Headers headers, String key) {
    var record = headers.lastHeader(key);
    if (record == null || record.value() == null) {
      return null;
    }
    return new String(record.value(), StandardCharsets.UTF_8);
  }
}
