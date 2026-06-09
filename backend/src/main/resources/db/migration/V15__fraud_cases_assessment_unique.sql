CREATE UNIQUE INDEX IF NOT EXISTS uq_fraud_cases_assessment
    ON identity_twin.fraud_cases (assessment_id)
    WHERE assessment_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_fraud_cases_assigned
    ON identity_twin.fraud_cases (assigned_to, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_fraud_cases_priority
    ON identity_twin.fraud_cases (priority, status, updated_at DESC);
