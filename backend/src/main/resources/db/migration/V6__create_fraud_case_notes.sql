SET
search_path TO identity_twin;

CREATE TABLE IF NOT EXISTS fraud_case_notes
(
    note_id
    TEXT
    PRIMARY
    KEY,
    challenge_id
    TEXT,
    assessment_id
    TEXT,
    customer_id
    TEXT
    NOT
    NULL,
    author
    TEXT
    NOT
    NULL
    DEFAULT
    'system',
    note_type
    TEXT
    NOT
    NULL
    DEFAULT
    'ANALYST',
    note_body
    TEXT
    NOT
    NULL,
    metadata_json
    JSONB
    NOT
    NULL
    DEFAULT
    '{}'
    :
    :
    jsonb,
    created_at
    TIMESTAMPTZ
    NOT
    NULL
    DEFAULT
    NOW
(
)
    );

CREATE INDEX IF NOT EXISTS idx_fraud_case_notes_challenge
    ON fraud_case_notes (challenge_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_fraud_case_notes_customer
    ON fraud_case_notes (customer_id, created_at DESC);
