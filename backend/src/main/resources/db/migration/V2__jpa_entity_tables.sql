SET
search_path TO identity_twin;

CREATE TABLE IF NOT EXISTS identity_twins
(
    customer_id
    TEXT
    PRIMARY
    KEY,
    transaction_count
    BIGINT
    NOT
    NULL
    DEFAULT
    0,
    amount_sum
    DOUBLE
    PRECISION
    NOT
    NULL
    DEFAULT
    0,
    amount_sum_of_squares
    DOUBLE
    PRECISION
    NOT
    NULL
    DEFAULT
    0,
    known_devices_csv
    TEXT,
    usual_countries_csv
    TEXT,
    merchant_category_counts_json
    TEXT,
    hour_histogram_csv
    TEXT,
    last_latitude
    DOUBLE
    PRECISION,
    last_longitude
    DOUBLE
    PRECISION,
    last_timestamp_epoch_seconds
    BIGINT,
    last_merchant_category
    TEXT,
    created_at
    TIMESTAMPTZ,
    updated_at
    TIMESTAMPTZ,
    version
    BIGINT
    NOT
    NULL
    DEFAULT
    0
);

CREATE TABLE IF NOT EXISTS fraud_decision_audit
(
    assessment_id
    TEXT
    PRIMARY
    KEY,
    transaction_id
    TEXT
    NOT
    NULL,
    customer_id
    TEXT
    NOT
    NULL,
    decision
    TEXT
    NOT
    NULL,
    final_score
    NUMERIC
(
    10,
    2
) NOT NULL,
    score_breakdown_json TEXT,
    reason_codes_json TEXT,
    event_snapshot_json TEXT,
    model_version TEXT,
    policy_version TEXT,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    assessed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
)
    );

CREATE INDEX IF NOT EXISTS idx_fraud_decision_audit_assessed_at
    ON fraud_decision_audit (assessed_at DESC);
