SET
search_path TO identity_twin;

CREATE TABLE IF NOT EXISTS twin_drift_events
(
    drift_event_id
    TEXT
    PRIMARY
    KEY,
    assessment_id
    TEXT
    NOT
    NULL,
    customer_id
    TEXT
    NOT
    NULL,
    transaction_id
    TEXT
    NOT
    NULL,
    drift_score
    NUMERIC
(
    10,
    2
) NOT NULL,
    drift_threshold NUMERIC
(
    10,
    2
) NOT NULL,
    signal_snapshot JSONB NOT NULL DEFAULT '[]'::jsonb,
    baseline_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
)
    );

CREATE INDEX IF NOT EXISTS idx_twin_drift_events_customer
    ON twin_drift_events (customer_id, detected_at DESC);

CREATE INDEX IF NOT EXISTS idx_twin_drift_events_detected_at
    ON twin_drift_events (detected_at DESC);
