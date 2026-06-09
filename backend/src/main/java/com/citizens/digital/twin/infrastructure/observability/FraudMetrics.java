package com.citizens.digital.twin.infrastructure.observability;

public final class FraudMetrics {
  public static final String DECISIONS_TOTAL = "fraud_decisions_total";
  public static final String DECISIONS_BLOCKED = "fraud_decisions_blocked_total";
  public static final String DECISIONS_CHALLENGED = "fraud_decisions_challenged_total";
  public static final String DECISION_LATENCY = "fraud_decision_latency_ms";
  public static final String MODEL_SCORE = "fraud_model_score_distribution";
  public static final String TWIN_DRIFT_SCORE = "fraud_twin_drift_score";
  public static final String MODEL_DRIFT_SCORE = "fraud_model_drift_score";
  public static final String MODEL_DRIFT_ALERT = "fraud_model_drift_alert";
  public static final String KAFKA_CONSUMER_LAG = "kafka_consumer_lag";
  public static final String STEP_UP_PENDING = "step_up_challenge_pending_count";
  public static final String AUDIT_WRITE_FAILURE = "audit_write_failure_count";
  public static final String DUPLICATE_EVENTS = "fraud_duplicate_events_total";

  private FraudMetrics() {}
}
