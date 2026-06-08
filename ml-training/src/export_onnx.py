"""
Export the trained LightGBM model to ONNX and verify the ONNX scores match the
native scores. ONNX is the hand-off to real-time serving: your Spring Boot scorer
loads fraud_model.onnx via ONNX Runtime (Java) and scores each Kafka event inline,
with no Python in the request path.
"""
from __future__ import annotations

import os

import lightgbm as lgb
import numpy as np
import onnxruntime as ort
from onnxmltools import convert_lightgbm
from onnxmltools.convert.common.data_types import FloatTensorType

import generate_data
from features import build_features, FEATURE_COLUMNS

ARTIFACTS = os.path.join(os.path.dirname(__file__), "..", "artifacts")


def main():
    model_path = os.path.join(ARTIFACTS, "fraud_model.txt")
    onnx_path = os.path.join(ARTIFACTS, "fraud_model.onnx")
    booster = lgb.Booster(model_file=model_path)

    initial_types = [("input", FloatTensorType([None, len(FEATURE_COLUMNS)]))]
    onnx_model = convert_lightgbm(booster, initial_types=initial_types,
                                  zipmap=False, target_opset=12)
    with open(onnx_path, "wb") as fh:
        fh.write(onnx_model.SerializeToString())
    print(f"Wrote {onnx_path}")

    # ---- Parity check: native LightGBM vs ONNX Runtime on a sample ----
    df = build_features(generate_data.generate(n_customers=200, seed=7))
    X = df[FEATURE_COLUMNS].astype(np.float32).values[:500]

    native = booster.predict(X)
    sess = ort.InferenceSession(onnx_path, providers=["CPUExecutionProvider"])
    out = sess.run(None, {"input": X})
    # zipmap=False -> probabilities array is the second output (shape n x 2).
    onnx_proba = out[1][:, 1]

    max_diff = float(np.max(np.abs(native - onnx_proba)))
    print(f"Feature order ({len(FEATURE_COLUMNS)}): {FEATURE_COLUMNS}")
    print(f"Max |native - onnx| over 500 rows: {max_diff:.3e}")
    assert max_diff < 1e-5, "ONNX export does not match native scores!"
    print("PARITY OK — ONNX model is safe to serve from the JVM scorer.")


if __name__ == "__main__":
    main()
