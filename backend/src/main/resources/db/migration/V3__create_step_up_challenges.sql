SET
search_path TO identity_twin;

CREATE TABLE IF NOT EXISTS step_up_challenges
(
    challenge_id
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
    challenge_type
    TEXT
    NOT
    NULL
    DEFAULT
    'OUT_OF_BAND',
    challenge_status
    TEXT
    NOT
    NULL
    DEFAULT
    'PENDING',
    delivery_channel
    TEXT
    NOT
    NULL
    DEFAULT
    'WEB',
    reason_code
    TEXT,
    reason_description
    TEXT,
    rule_score
    NUMERIC
(
    10,
    2
),
    ml_score NUMERIC
(
    10,
    2
),
    final_risk_score NUMERIC
(
    10,
    2
) NOT NULL,
    explainability_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    expires_at TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    expired_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW
(
)
    );

CREATE INDEX IF NOT EXISTS idx_step_up_challenges_status
    ON step_up_challenges (challenge_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_step_up_challenges_customer
    ON step_up_challenges (customer_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_step_up_challenges_assessment
    ON step_up_challenges (assessment_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_step_up_challenges_assessment_pending
    ON step_up_challenges (assessment_id)
    WHERE challenge_status = 'PENDING';
