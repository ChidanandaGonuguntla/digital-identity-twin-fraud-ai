ALTER TABLE identity_twin.fraud_decision_audit
    ADD COLUMN IF NOT EXISTS champion_score NUMERIC (10, 4),
    ADD COLUMN IF NOT EXISTS challenger_score NUMERIC (10, 4),
    ADD COLUMN IF NOT EXISTS score_delta NUMERIC (10, 4),
    ADD COLUMN IF NOT EXISTS model_agreement BOOLEAN,
    ADD COLUMN IF NOT EXISTS champion_model_version VARCHAR (100),
    ADD COLUMN IF NOT EXISTS challenger_model_version VARCHAR (100);

ALTER TABLE identity_twin.model_registry
    ADD COLUMN IF NOT EXISTS model_role VARCHAR (20) NOT NULL DEFAULT 'CANDIDATE';

UPDATE identity_twin.model_registry
SET model_role = CASE
                     WHEN registry_status = 'ACTIVE' THEN 'CHAMPION'
                     WHEN registry_status IN ('DEPRECATED', 'ROLLED_BACK') THEN 'RETIRED'
                     ELSE 'CANDIDATE'
    END
WHERE model_name = 'digital-twin-fraud-risk';

CREATE TABLE IF NOT EXISTS identity_twin.analyst_feedback
(
    feedback_id
    VARCHAR
(
    100
) PRIMARY KEY,
    assessment_id VARCHAR
(
    100
) NOT NULL,
    transaction_id VARCHAR
(
    100
) NOT NULL,
    customer_id VARCHAR
(
    100
) NOT NULL,
    outcome VARCHAR
(
    40
) NOT NULL,
    analyst_id VARCHAR
(
    255
) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    CONSTRAINT uq_analyst_feedback_assessment UNIQUE
(
    assessment_id
)
    );

CREATE INDEX IF NOT EXISTS idx_analyst_feedback_customer ON identity_twin.analyst_feedback (customer_id, created_at DESC);

CREATE TABLE IF NOT EXISTS identity_twin.fraud_cases
(
    case_id
    VARCHAR
(
    100
) PRIMARY KEY,
    assessment_id VARCHAR
(
    100
),
    transaction_id VARCHAR
(
    100
) NOT NULL,
    customer_id VARCHAR
(
    100
) NOT NULL,
    status VARCHAR
(
    40
) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR
(
    20
) NOT NULL DEFAULT 'MEDIUM',
    assigned_to VARCHAR
(
    255
),
    sla_due_at TIMESTAMPTZ,
    escalation_level INT NOT NULL DEFAULT 0,
    closure_reason VARCHAR
(
    80
),
    summary TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    closed_at TIMESTAMPTZ
    );

CREATE INDEX IF NOT EXISTS idx_fraud_cases_status ON identity_twin.fraud_cases (status, priority, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_cases_customer ON identity_twin.fraud_cases (customer_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS identity_twin.fraud_case_events
(
    event_id
    VARCHAR
(
    100
) PRIMARY KEY,
    case_id VARCHAR
(
    100
) NOT NULL REFERENCES identity_twin.fraud_cases
(
    case_id
),
    event_type VARCHAR
(
    60
) NOT NULL,
    actor_id VARCHAR
(
    255
),
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
)
    );

CREATE INDEX IF NOT EXISTS idx_fraud_case_events_case ON identity_twin.fraud_case_events (case_id, created_at DESC);

CREATE TABLE IF NOT EXISTS identity_twin.customer_velocity_features
(
    customer_id
    VARCHAR
(
    100
) PRIMARY KEY,
    txn_count_5m INT NOT NULL DEFAULT 0,
    txn_count_1h INT NOT NULL DEFAULT 0,
    txn_count_24h INT NOT NULL DEFAULT 0,
    amount_sum_1h NUMERIC
(
    14,
    2
) NOT NULL DEFAULT 0,
    new_devices_24h INT NOT NULL DEFAULT 0,
    countries_24h INT NOT NULL DEFAULT 0,
    category_changes_10m INT NOT NULL DEFAULT 0,
    failed_attempts_30m INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
)
    );

CREATE TABLE IF NOT EXISTS identity_twin.feature_store_values
(
    entity_key
    VARCHAR
(
    160
) PRIMARY KEY,
    feature_name VARCHAR
(
    120
) NOT NULL,
    feature_value NUMERIC
(
    14,
    6
) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
)
    );

CREATE INDEX IF NOT EXISTS idx_feature_store_name ON identity_twin.feature_store_values (feature_name, updated_at DESC);
