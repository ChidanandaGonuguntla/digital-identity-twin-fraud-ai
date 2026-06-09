ALTER TABLE identity_twin.fraud_decision_audit
    ADD COLUMN IF NOT EXISTS amount NUMERIC (14, 2),
    ADD COLUMN IF NOT EXISTS merchant_category VARCHAR (120),
    ADD COLUMN IF NOT EXISTS device_id VARCHAR (120);

UPDATE identity_twin.fraud_decision_audit
SET amount            = COALESCE(amount, (event_snapshot_json ->> 'amount')::numeric),
    merchant_category = COALESCE(NULLIF(merchant_category, ''), event_snapshot_json ->> 'merchantCategory'),
    device_id         = COALESCE(NULLIF(device_id, ''), event_snapshot_json ->> 'deviceId')
WHERE jsonb_typeof(event_snapshot_json) = 'object';

CREATE INDEX IF NOT EXISTS idx_fraud_decision_audit_merchant_category
    ON identity_twin.fraud_decision_audit (merchant_category, assessed_at DESC);
