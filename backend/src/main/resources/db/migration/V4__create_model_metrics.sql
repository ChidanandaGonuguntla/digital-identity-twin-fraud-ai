SET
search_path TO identity_twin;

CREATE TABLE IF NOT EXISTS model_metrics
(
    metric_id
    TEXT
    PRIMARY
    KEY,
    assessment_id
    TEXT,
    model_name
    TEXT
    NOT
    NULL,
    model_version
    TEXT
    NOT
    NULL,
    precision_score
    NUMERIC
(
    8,
    4
),
    recall_score NUMERIC
(
    8,
    4
),
    auc_score NUMERIC
(
    8,
    4
),
    f1_score NUMERIC
(
    8,
    4
),
    drift_score NUMERIC
(
    8,
    4
),
    latency_ms BIGINT NOT NULL DEFAULT 0,
    feature_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
)
    );

CREATE INDEX IF NOT EXISTS idx_model_metrics_recorded_at
    ON model_metrics (recorded_at DESC);

CREATE INDEX IF NOT EXISTS idx_model_metrics_model_version
    ON model_metrics (model_version, recorded_at DESC);
