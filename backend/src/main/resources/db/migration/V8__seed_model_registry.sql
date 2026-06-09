SET
search_path TO identity_twin;

INSERT INTO model_registry (model_name, model_version, artifact_path, feature_order, metrics, trained_at, deployed_at,
                            active)
VALUES ('digital-twin-fraud-risk',
        'fraud-risk-v1.0.0',
        'classpath:models/fraud-risk-v1.0.0.onnx',
        '["amount_log","amount_zscore","hour_of_day_norm","is_new_device","is_new_country","is_high_risk_channel","merchant_risk_score","category_frequency","twin_txn_count_norm","geo_distance_norm"]'::jsonb,
        '{"auc":0.0,"precision":0.0,"recall":0.0}'::jsonb,
        NOW(),
        NOW(),
        TRUE) ON CONFLICT (model_name, model_version) DO NOTHING;

INSERT INTO model_registry (model_name, model_version, artifact_path, feature_order, metrics, trained_at, deployed_at,
                            active)
VALUES ('digital-twin-fraud-risk',
        'fraud-risk-v0.9.0',
        'classpath:models/fraud-risk-v0.9.0.onnx',
        '["amount_log","amount_zscore","hour_of_day_norm","is_new_device","is_new_country","is_high_risk_channel","merchant_risk_score","category_frequency","twin_txn_count_norm","geo_distance_norm"]'::jsonb,
        '{"auc":0.0,"precision":0.0,"recall":0.0}'::jsonb,
        NOW(),
        NOW(),
        FALSE) ON CONFLICT (model_name, model_version) DO NOTHING;
