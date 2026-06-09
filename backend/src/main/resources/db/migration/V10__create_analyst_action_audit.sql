CREATE TABLE IF NOT EXISTS identity_twin.analyst_action_audit
(
    action_id
    TEXT
    PRIMARY
    KEY,
    actor_email
    TEXT
    NOT
    NULL,
    actor_role
    TEXT
    NOT
    NULL,
    action_type
    TEXT
    NOT
    NULL,
    resource_type
    TEXT,
    resource_id
    TEXT,
    details_json
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

CREATE INDEX IF NOT EXISTS idx_analyst_action_audit_created
    ON identity_twin.analyst_action_audit (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_analyst_action_audit_actor
    ON identity_twin.analyst_action_audit (actor_email, created_at DESC);
