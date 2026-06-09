ALTER TABLE identity_twin.fraud_decision_audit
    ADD COLUMN IF NOT EXISTS feature_version TEXT,
    ADD COLUMN IF NOT EXISTS final_decision_reason TEXT,
    ADD COLUMN IF NOT EXISTS feature_vector_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS challenged BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS twin_updated BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_fraud_decision_audit_transaction
    ON identity_twin.fraud_decision_audit (transaction_id, assessed_at DESC);

CREATE INDEX IF NOT EXISTS idx_fraud_decision_audit_assessed_at
    ON identity_twin.fraud_decision_audit (assessed_at DESC);
