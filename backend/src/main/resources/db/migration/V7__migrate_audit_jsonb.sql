SET
search_path TO identity_twin;

ALTER TABLE fraud_decision_audit
ALTER
COLUMN score_breakdown_json TYPE JSONB
        USING CASE
            WHEN score_breakdown_json IS NULL OR btrim(score_breakdown_json) = '' THEN '{}'::jsonb
            ELSE score_breakdown_json::jsonb
END;

ALTER TABLE fraud_decision_audit
ALTER
COLUMN reason_codes_json TYPE JSONB
        USING CASE
            WHEN reason_codes_json IS NULL OR btrim(reason_codes_json) = '' THEN '[]'::jsonb
            ELSE reason_codes_json::jsonb
END;

ALTER TABLE fraud_decision_audit
ALTER
COLUMN event_snapshot_json TYPE JSONB
        USING CASE
            WHEN event_snapshot_json IS NULL OR btrim(event_snapshot_json) = '' THEN '{}'::jsonb
            ELSE event_snapshot_json::jsonb
END;

ALTER TABLE fraud_decision_audit
    ALTER COLUMN score_breakdown_json SET DEFAULT '{}'::jsonb,
ALTER
COLUMN reason_codes_json SET DEFAULT '[]'::jsonb,
    ALTER
COLUMN event_snapshot_json SET DEFAULT '{}'::jsonb;

CREATE INDEX IF NOT EXISTS idx_fraud_decision_audit_decision
    ON fraud_decision_audit (decision, assessed_at DESC);

CREATE INDEX IF NOT EXISTS idx_fraud_decision_audit_customer
    ON fraud_decision_audit (customer_id, assessed_at DESC);
