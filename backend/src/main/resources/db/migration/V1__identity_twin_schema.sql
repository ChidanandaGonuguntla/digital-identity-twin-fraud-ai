-- =============================================================================
-- V1__identity_twin_schema.sql
-- Enterprise Digital Identity Twin — Fraud AI Platform
--
-- PostgreSQL schema backing the digital-twin behavioral baselines and the
-- deterministic + ML fraud decision record (the identity_twin.ml_fraud_score
-- table from the architecture diagram).
--
-- Design rules encoded here:
--   * The ML model contributes a PROBABILITY only (ml_fraud_probability).
--   * The FINAL decision is produced by the deterministic Risk Fusion Engine:
--       final_risk_score = rule_score * rule_weight + ml_score * ml_weight
--   * Every decision persists its inputs (features), outputs, the model identity,
--     the fusion weights, and inference latency — for audit and replay.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS identity_twin;

SET search_path TO identity_twin;

-- -----------------------------------------------------------------------------
-- 1. customer_twin — the behavioral baseline (the digital twin itself)
--    Running aggregates allow O(1) incremental updates without retaining raw history.
-- -----------------------------------------------------------------------------
CREATE TABLE customer_twin (
    customer_id        TEXT PRIMARY KEY,
    transaction_count  BIGINT        NOT NULL DEFAULT 0,
    amount_sum         NUMERIC(18,2) NOT NULL DEFAULT 0,   -- running Σ amount
    amount_sum_sq      NUMERIC(26,4) NOT NULL DEFAULT 0,   -- running Σ amount² (for std-dev)
    -- hour-of-day histogram, category counts, last geo + time, etc.
    baseline           JSONB         NOT NULL DEFAULT '{}'::jsonb,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    last_updated       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

COMMENT ON TABLE  customer_twin            IS 'Behavioral digital twin: per-customer running baseline used for deviation scoring.';
COMMENT ON COLUMN customer_twin.baseline   IS 'JSONB: {hourHistogram[24], categoryCounts{}, lastLat, lastLon, lastTs}.';

-- Derived statistics (mean/std-dev) without storing redundant columns.
CREATE VIEW customer_twin_stats AS
SELECT
    customer_id,
    transaction_count,
    CASE WHEN transaction_count > 0
         THEN ROUND(amount_sum / transaction_count, 2) ELSE 0 END                     AS mean_amount,
    CASE WHEN transaction_count > 1
         THEN ROUND(SQRT(GREATEST(amount_sum_sq / transaction_count
              - POWER(amount_sum / transaction_count, 2), 0))::numeric, 2) ELSE 0 END AS std_dev_amount,
    last_updated
FROM customer_twin;

-- -----------------------------------------------------------------------------
-- 2. known_device / known_beneficiary — entity novelty checks ("Known Devices",
--    "Known Beneficiaries" in the Digital Identity Twin Service)
-- -----------------------------------------------------------------------------
CREATE TABLE known_device (
    customer_id  TEXT        NOT NULL REFERENCES customer_twin(customer_id) ON DELETE CASCADE,
    device_id    TEXT        NOT NULL,
    first_seen   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen    TIMESTAMPTZ NOT NULL DEFAULT now(),
    use_count    BIGINT      NOT NULL DEFAULT 1,
    PRIMARY KEY (customer_id, device_id)
);

CREATE TABLE known_beneficiary (
    customer_id     TEXT        NOT NULL REFERENCES customer_twin(customer_id) ON DELETE CASCADE,
    beneficiary_id  TEXT        NOT NULL,
    first_seen      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen       TIMESTAMPTZ NOT NULL DEFAULT now(),
    transfer_count  BIGINT      NOT NULL DEFAULT 1,
    PRIMARY KEY (customer_id, beneficiary_id)
);

-- -----------------------------------------------------------------------------
-- 3. transaction — the raw inbound event log (what the Feature Builder reads)
-- -----------------------------------------------------------------------------
CREATE TABLE transaction (
    transaction_id     TEXT PRIMARY KEY,
    customer_id        TEXT          NOT NULL,
    amount             NUMERIC(14,2) NOT NULL,
    merchant_category  TEXT,
    device_id          TEXT,
    beneficiary_id     TEXT,
    latitude           DOUBLE PRECISION,
    longitude          DOUBLE PRECISION,
    merch_latitude     DOUBLE PRECISION,
    merch_longitude    DOUBLE PRECISION,
    channel            TEXT,                                  -- card / online / wire ...
    event_time         TIMESTAMPTZ   NOT NULL,
    ingested_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_transaction_customer_time ON transaction (customer_id, event_time DESC);

-- -----------------------------------------------------------------------------
-- 4. model_registry — which ONNX model is deployed ("Deploy fraud_model.onnx")
-- -----------------------------------------------------------------------------
CREATE TABLE model_registry (
    model_name     TEXT        NOT NULL,
    model_version  TEXT        NOT NULL,
    artifact_path  TEXT        NOT NULL,                     -- e.g. /models/fraud_model.onnx
    feature_order  JSONB       NOT NULL,                     -- ordered FEATURE_COLUMNS the model expects
    metrics        JSONB,                                    -- AUC-PR, recall@FPR, etc.
    trained_at     TIMESTAMPTZ,
    deployed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    active         BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (model_name, model_version)
);

-- Only one active model at a time.
CREATE UNIQUE INDEX uq_model_registry_active ON model_registry (active) WHERE active;

-- -----------------------------------------------------------------------------
-- 5. ml_fraud_score — THE decision record (matches the diagram exactly,
--    plus persisted features / weights / latency for audit & replay)
-- -----------------------------------------------------------------------------
CREATE TABLE ml_fraud_score (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id        TEXT          NOT NULL REFERENCES transaction(transaction_id),
    customer_id           TEXT          NOT NULL,

    -- Deterministic tier
    rule_score            NUMERIC(6,2)  NOT NULL,            -- 0–100 from DeviationScoringEngine

    -- ML tier (probability ONLY — never the final decision)
    ml_fraud_probability  NUMERIC(7,6),                      -- 0.000000–1.000000 from ONNX model
    ml_score              NUMERIC(6,2),                      -- probability scaled to 0–100

    -- Risk Fusion Engine output
    rule_weight           NUMERIC(4,3)  NOT NULL DEFAULT 0.500,
    ml_weight             NUMERIC(4,3)  NOT NULL DEFAULT 0.500,
    final_risk_score      NUMERIC(6,2)  NOT NULL,            -- rule*rule_weight + ml*ml_weight
    final_decision        TEXT          NOT NULL
                            CHECK (final_decision IN ('APPROVE','REVIEW','DECLINE')),

    -- Model identity
    model_name            TEXT,
    model_version         TEXT,

    -- Explainability + audit
    top_risk_signals      JSONB,                             -- [{name, contribution}, ...] (SHAP / rule reasons)
    features              JSONB,                             -- the feature vector scored (replayable)
    cold_start            BOOLEAN       NOT NULL DEFAULT FALSE,
    inference_latency_ms  INTEGER,                           -- ONNX inference time
    total_latency_ms      INTEGER,                           -- end-to-end evaluate() time
    decided_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_mlscore_customer    ON ml_fraud_score (customer_id, decided_at DESC);
CREATE INDEX idx_mlscore_decision    ON ml_fraud_score (final_decision, decided_at DESC);
CREATE INDEX idx_mlscore_decided_at  ON ml_fraud_score (decided_at DESC);

COMMENT ON TABLE  ml_fraud_score                      IS 'Per-transaction fraud decision: deterministic rule score + ML probability fused into a final decision.';
COMMENT ON COLUMN ml_fraud_score.ml_fraud_probability IS 'Model output is a probability only; it never decides on its own.';
COMMENT ON COLUMN ml_fraud_score.final_decision       IS 'APPROVE / REVIEW / DECLINE, from the deterministic Risk Fusion Engine.';

-- -----------------------------------------------------------------------------
-- 6. audit_log — append-only audit trail ("Persist audit ... decisions")
-- -----------------------------------------------------------------------------
CREATE TABLE audit_log (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type  TEXT        NOT NULL,                        -- 'TRANSACTION' | 'DECISION' | 'MODEL'
    entity_id    TEXT        NOT NULL,
    action       TEXT        NOT NULL,
    detail       JSONB,
    actor        TEXT        NOT NULL DEFAULT 'system',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);

CREATE TABLE IF NOT EXISTS identity_twin.fraud_step_up_challenge (
                                                                     challenge_id UUID PRIMARY KEY,
                                                                     fraud_event_id UUID NOT NULL,
                                                                     customer_id VARCHAR(100) NOT NULL,
    account_id VARCHAR(100),
    transaction_id VARCHAR(100),

    challenge_type VARCHAR(60) NOT NULL,
    challenge_status VARCHAR(40) NOT NULL,

    delivery_channel VARCHAR(60) NOT NULL,
    destination_label VARCHAR(150),

    reason_code VARCHAR(100),
    reason_description VARCHAR(500),

    rule_score NUMERIC(6,2),
    ml_score NUMERIC(6,2),
    final_risk_score NUMERIC(6,2),

    approval_token_hash VARCHAR(255),
    expires_at TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ,
    denied_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_step_up_challenge_event
    ON identity_twin.fraud_step_up_challenge(fraud_event_id);

CREATE INDEX IF NOT EXISTS idx_step_up_challenge_customer
    ON identity_twin.fraud_step_up_challenge(customer_id);

CREATE INDEX IF NOT EXISTS idx_step_up_challenge_status
    ON identity_twin.fraud_step_up_challenge(challenge_status);
