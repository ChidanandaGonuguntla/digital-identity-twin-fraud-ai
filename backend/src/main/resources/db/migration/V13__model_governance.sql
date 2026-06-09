ALTER TABLE identity_twin.model_registry
    ADD COLUMN IF NOT EXISTS training_dataset_version VARCHAR (80),
    ADD COLUMN IF NOT EXISTS feature_schema_version VARCHAR (80),
    ADD COLUMN IF NOT EXISTS registry_status VARCHAR (40) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN IF NOT EXISTS approved_by VARCHAR (255),
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS bias_review_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

UPDATE identity_twin.model_registry
SET training_dataset_version = COALESCE(training_dataset_version, 'fraud-synthetic-v1.0.0'),
    feature_schema_version   = COALESCE(feature_schema_version, 'fraud-features-v1.0.0'),
    registry_status          = CASE WHEN active THEN 'ACTIVE' ELSE 'APPROVED' END,
    approved_by              = COALESCE(approved_by, 'model-risk-committee@citizens.com'),
    approved_at              = COALESCE(approved_at, deployed_at),
    bias_review_json         = COALESCE(
            NULLIF(bias_review_json, '{}'::jsonb),
            '{"reviewStatus":"PENDING_SCHEDULED","notes":"Bias and fairness review scheduled with model risk."}' ::jsonb
                               )
WHERE model_name = 'digital-twin-fraud-risk';

CREATE TABLE IF NOT EXISTS identity_twin.feature_schema_registry
(
    feature_schema_version
    VARCHAR
(
    80
) PRIMARY KEY,
    model_name VARCHAR
(
    120
) NOT NULL,
    feature_order JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    active BOOLEAN NOT NULL DEFAULT FALSE
    );

INSERT INTO identity_twin.feature_schema_registry (feature_schema_version, model_name, feature_order, active)
VALUES ('fraud-features-v1.0.0',
        'digital-twin-fraud-risk',
        '["amount_log","amount_zscore","hour_of_day_norm","is_new_device","is_new_country","is_high_risk_channel","merchant_risk_score","category_frequency","twin_txn_count_norm","geo_distance_norm"]'::jsonb,
        TRUE) ON CONFLICT (feature_schema_version) DO NOTHING;
