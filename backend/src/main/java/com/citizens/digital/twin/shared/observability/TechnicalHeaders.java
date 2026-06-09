package com.citizens.digital.twin.shared.observability;

public final class TechnicalHeaders {
  public static final String CORRELATION_ID = "X-Correlation-Id";
  public static final String TRACE_ID = "X-Trace-Id";
  public static final String SOURCE_SYSTEM = "X-Source-System";
  public static final String EVENT_ID = "X-Event-Id";
  public static final String EVENT_TIMESTAMP = "X-Event-Timestamp";

  private TechnicalHeaders() {}
}
