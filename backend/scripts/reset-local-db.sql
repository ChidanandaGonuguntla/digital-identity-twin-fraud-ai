SET
search_path TO identity_twin;

DO
$$
DECLARE
r RECORD;
BEGIN
FOR r IN (
    SELECT tablename
    FROM pg_tables
    WHERE schemaname = 'identity_twin'
      AND tablename <> 'flyway_schema_history'
  ) LOOP
    EXECUTE format('TRUNCATE TABLE identity_twin.%I RESTART IDENTITY CASCADE', r.tablename);
END LOOP;
END $$;

INSERT INTO model_registry (model_name, model_version, artifact_path, feature_order, metrics, trained_at, deployed_at,
                            active, training_dataset_version, feature_schema_version, registry_status, approved_by,
                            approved_at)
VALUES ('digital-twin-fraud-risk',
        'fraud-risk-v1.0.0',
        'classpath:models/fraud-risk-v1.0.0.onnx',
        '["amount_log","amount_zscore","hour_of_day_norm","is_new_device","is_new_country","is_high_risk_channel","merchant_risk_score","category_frequency","twin_txn_count_norm","geo_distance_norm"]'::jsonb,
        '{"auc":1.0,"precision":1.0,"recall":1.0,"f1Score":1.0,"falsePositiveRate":0.0}'::jsonb,
        NOW(),
        NOW(),
        TRUE,
        'fraud-synthetic-v1.0.0',
        'fraud-features-v1.0.0',
        'ACTIVE',
        'model-risk-committee@citizens.com',
        NOW()) ON CONFLICT (model_name, model_version) DO
UPDATE SET
    active = EXCLUDED.active,
    registry_status = EXCLUDED.registry_status,
    deployed_at = NOW();

INSERT INTO model_registry (model_name, model_version, artifact_path, feature_order, metrics, trained_at, deployed_at,
                            active, training_dataset_version, feature_schema_version, registry_status, approved_by,
                            approved_at)
VALUES ('digital-twin-fraud-risk',
        'fraud-risk-v0.9.0',
        'classpath:models/fraud-risk-v0.9.0.onnx',
        '["amount_log","amount_zscore","hour_of_day_norm","is_new_device","is_new_country","is_high_risk_channel","merchant_risk_score","category_frequency","twin_txn_count_norm","geo_distance_norm"]'::jsonb,
        '{"auc":0.982,"precision":0.971,"recall":0.964,"f1Score":0.967,"falsePositiveRate":0.021}'::jsonb,
        NOW(),
        NOW(),
        FALSE,
        'fraud-synthetic-v0.9.0',
        'fraud-features-v1.0.0',
        'APPROVED',
        'model-risk-committee@citizens.com',
        NOW()) ON CONFLICT (model_name, model_version) DO NOTHING;

INSERT INTO feature_schema_registry (feature_schema_version, model_name, feature_order, active)
VALUES ('fraud-features-v1.0.0',
        'digital-twin-fraud-risk',
        '["amount_log","amount_zscore","hour_of_day_norm","is_new_device","is_new_country","is_high_risk_channel","merchant_risk_score","category_frequency","twin_txn_count_norm","geo_distance_norm"]'::jsonb,
        TRUE) ON CONFLICT (feature_schema_version) DO NOTHING;
