DELETE
FROM identity_twin.fraud_decision_audit
WHERE assessment_id IN (SELECT assessment_id
                        FROM (SELECT assessment_id,
                                     ROW_NUMBER() OVER (
                   PARTITION BY transaction_id
                   ORDER BY assessed_at DESC, created_at DESC, assessment_id DESC
               ) AS row_num
                              FROM identity_twin.fraud_decision_audit) ranked
                        WHERE row_num > 1);

CREATE UNIQUE INDEX IF NOT EXISTS ux_fraud_audit_transaction_id
    ON identity_twin.fraud_decision_audit (transaction_id);
