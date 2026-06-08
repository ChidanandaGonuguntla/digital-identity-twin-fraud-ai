"""
Train a fraud model the right way for an imbalanced, time-ordered problem.

Key decisions (all deliberate for fraud):
  * TIME-BASED split, never random — train on the earliest 70% of time, validate on
    the next 15%, test on the most recent 15% (out-of-time). Random splits leak the
    future and inflate metrics.
  * Cost-weighting via scale_pos_weight instead of SMOTE — synthetic oversampling
    tends to hurt on real fraud data.
  * Metrics that matter under 0.8% prevalence: AUC-PR (average precision) and
    recall at a fixed false-positive budget. Accuracy and plain ROC-AUC are misleading.
  * Explainability via LightGBM's native SHAP (pred_contrib) — exact for trees, no
    extra dependency. In banking, every decision needs reason codes.
"""
from __future__ import annotations

import json
import os

import lightgbm as lgb
import numpy as np
import pandas as pd
from sklearn.metrics import average_precision_score, roc_auc_score, precision_recall_curve

import generate_data
from features import build_features, FEATURE_COLUMNS

ARTIFACTS = os.path.join(os.path.dirname(__file__), "..", "artifacts")


def time_split(df: pd.DataFrame, train_frac=0.70, valid_frac=0.15):
    df = df.sort_values("unix_time").reset_index(drop=True)
    n = len(df)
    t_end = int(n * train_frac)
    v_end = int(n * (train_frac + valid_frac))
    return df.iloc[:t_end], df.iloc[t_end:v_end], df.iloc[v_end:]


def recall_at_fpr(y_true, y_score, max_fpr=0.01):
    """Recall achievable while holding the false-positive rate at or below max_fpr."""
    order = np.argsort(-y_score)
    y_true = np.asarray(y_true)[order]
    neg = (y_true == 0).sum()
    pos = (y_true == 1).sum()
    fp = np.cumsum(y_true == 0)
    tp = np.cumsum(y_true == 1)
    allowed = fp <= max_fpr * neg
    return float(tp[allowed].max() / pos) if allowed.any() and pos > 0 else 0.0


def main():
    os.makedirs(ARTIFACTS, exist_ok=True)
    print("Generating data (swap for generate_data.load_sparkov_csv(path) to use the real set)...")
    df = generate_data.generate()
    print(f"  {len(df):,} transactions, fraud rate {df['is_fraud'].mean():.4f}")

    print("Building causal twin features...")
    df = build_features(df)

    train, valid, test = time_split(df)
    print(f"  train={len(train):,}  valid={len(valid):,}  test(out-of-time)={len(test):,}")

    X_tr, y_tr = train[FEATURE_COLUMNS], train["is_fraud"]
    X_va, y_va = valid[FEATURE_COLUMNS], valid["is_fraud"]
    X_te, y_te = test[FEATURE_COLUMNS], test["is_fraud"]

    spw = float((y_tr == 0).sum() / max((y_tr == 1).sum(), 1))
    print(f"  scale_pos_weight = {spw:.1f}")

    params = dict(
        objective="binary",
        metric="average_precision",
        learning_rate=0.05,
        num_leaves=48,
        min_child_samples=50,
        feature_fraction=0.85,
        bagging_fraction=0.85,
        bagging_freq=1,
        scale_pos_weight=spw,
        verbosity=-1,
        seed=42,
    )
    dtrain = lgb.Dataset(X_tr, label=y_tr)
    dvalid = lgb.Dataset(X_va, label=y_va, reference=dtrain)
    model = lgb.train(
        params, dtrain, num_boost_round=600,
        valid_sets=[dvalid], valid_names=["valid"],
        callbacks=[lgb.early_stopping(40), lgb.log_evaluation(0)],
    )

    # ---- Evaluate on the out-of-time test set ----
    p_te = model.predict(X_te)
    metrics = {
        "test_auc_pr": float(average_precision_score(y_te, p_te)),
        "test_roc_auc": float(roc_auc_score(y_te, p_te)),
        "recall_at_1pct_fpr": recall_at_fpr(y_te, p_te, 0.01),
        "recall_at_0p1pct_fpr": recall_at_fpr(y_te, p_te, 0.001),
        "test_fraud_rate": float(y_te.mean()),
        "best_iteration": model.best_iteration,
        "n_train": len(train), "n_test": len(test),
    }

    # Pick an operating threshold from the validation PR curve (target ~precision 0.5).
    pv = model.predict(X_va)
    prec, rec, thr = precision_recall_curve(y_va, pv)
    target = np.where(prec[:-1] >= 0.5)[0]
    op_threshold = float(thr[target[0]]) if len(target) else 0.5
    metrics["operating_threshold"] = op_threshold

    # ---- Native SHAP global importance (exact, tree-based) ----
    contribs = model.predict(X_te, pred_contrib=True)  # shape (n, n_features+1)
    shap_abs = np.abs(contribs[:, :-1]).mean(axis=0)
    importance = sorted(zip(FEATURE_COLUMNS, shap_abs.tolist()),
                        key=lambda x: x[1], reverse=True)
    metrics["shap_global_importance"] = [
        {"feature": f, "mean_abs_shap": round(v, 5)} for f, v in importance]

    # ---- Persist artifacts ----
    model.save_model(os.path.join(ARTIFACTS, "fraud_model.txt"))
    with open(os.path.join(ARTIFACTS, "metrics.json"), "w") as fh:
        json.dump(metrics, fh, indent=2)

    print("\n=== Out-of-time test performance ===")
    print(f"  AUC-PR (avg precision): {metrics['test_auc_pr']:.4f}   (baseline = fraud rate {metrics['test_fraud_rate']:.4f})")
    print(f"  ROC-AUC:                {metrics['test_roc_auc']:.4f}")
    print(f"  Recall @ 1% FPR:        {metrics['recall_at_1pct_fpr']:.4f}")
    print(f"  Recall @ 0.1% FPR:      {metrics['recall_at_0p1pct_fpr']:.4f}")
    print(f"  Operating threshold:    {op_threshold:.4f}")
    print("\n  Top fraud drivers (mean |SHAP|):")
    for row in metrics["shap_global_importance"][:8]:
        print(f"    {row['feature']:<22} {row['mean_abs_shap']:.4f}")
    print(f"\nArtifacts written to {os.path.abspath(ARTIFACTS)}")


if __name__ == "__main__":
    main()
