package com.citizens.digital.twin.shared.observability;

public final class ObservabilityContext {
  public static final String MDC_CORRELATION_ID = "correlationId";
  public static final String MDC_TRACE_ID = "traceId";
  public static final String MDC_SOURCE_SYSTEM = "sourceSystem";
  public static final String MDC_EVENT_ID = "eventId";
  public static final String MDC_EVENT_TIMESTAMP = "eventTimestamp";

  private ObservabilityContext() {}
}
