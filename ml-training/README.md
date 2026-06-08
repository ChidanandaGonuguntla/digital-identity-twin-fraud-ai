# Fraud Model Training → Real-Time Prediction

An end-to-end, **runnable** pipeline that trains a fraud model on 2 years of
transaction history and exports it as an **ONNX** model your real-time Spring Boot
scorer loads to predict each incoming transaction. It is the bridge between the
historical dataset and the live Kafka/Valkey serving path.

## The dataset

| Dataset                         | Get it from                                                                                         | Use it for                                                                                        |
|---------------------------------|-----------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| **Sparkov** (recommended start) | Kaggle `kartik2112/fraud-detection`, or generate via the open-source `Sparkov_Data_Generation` repo | The twin approach — it has `cc_num`, `amt`, `category`, `lat/long`, `merch_lat/long`, `unix_time` |
| **IEEE-CIS**                    | Kaggle `ieee-fraud-detection`                                                                       | Most realistic supervised training (device, email, card signals)                                  |
| **BAF**                         | Kaggle / NeurIPS 2022 Feedzai                                                                       | The **new-account / cold-start** fraud case specifically                                          |

**No dataset is "real-time."** You train on the historical file, then the *same*
features are computed live per transaction to predict it. This repo ships a
synthetic generator (Sparkov-compatible schema) so it runs immediately; swap in the
real CSV by replacing one call:

```python
# in train.py / export_onnx.py
df = generate_data.load_sparkov_csv("/path/to/fraudTrain.csv")   # instead of generate()
```

## Pipeline

```
generate_data.py   2yr labeled transactions (real Sparkov schema; synthetic for demo)
       │
features.py        CAUSAL twin features — per customer, prior behavior only (no leakage)
       │           geo-velocity · personal amount z-score · velocity windows ·
       │           device/category novelty · time-since-previous
       ▼
train.py           TIME-based split (70/15/15, out-of-time test)
       │           cost-weighted LightGBM (scale_pos_weight, not SMOTE)
       │           metrics: AUC-PR + recall@fixed-FPR  ·  native SHAP reason codes
       ▼
export_onnx.py     LightGBM → ONNX, verified to score identically (parity check)
       ▼
artifacts/fraud_model.onnx   →  loaded by your Spring Boot scorer (ONNX Runtime Java)
```

## Run it

```bash
pip install -r requirements.txt
python src/train.py          # trains, evaluates out-of-time, writes artifacts/
python src/export_onnx.py    # exports ONNX + verifies native/ONNX parity
```

## Demo results (synthetic data — optimistic; real data lands lower)

- AUC-PR ≈ 0.97 (baseline = 0.8% fraud rate), recall ≈ 0.98 at a 1% false-positive budget.
- Top SHAP drivers: `geo_velocity_kmh`, `is_new_category`, `is_new_device`,
  `secs_since_prev`, `cust_txn_seq` — i.e. the digital-twin signals.
- On real Sparkov/IEEE-CIS expect AUC-PR ~0.7–0.85. The methodology transfers; the
  inflated synthetic numbers do not.

## Why these choices matter for fraud (not generic ML)

- **Causal features.** Each feature uses only the customer's *past*. Any leakage of
  future info makes a model that wins offline and loses live. This is the #1 failure.
- **Time split, not random.** Train on early months, test on the most recent —
  out-of-time, the way the model will actually be used.
- **AUC-PR & recall@FPR, not accuracy.** At 0.8% prevalence, accuracy is meaningless;
  you tune the threshold against the cost of a missed fraud vs. a false decline.
- **Cost-weighting over SMOTE.** Synthetic oversampling usually hurts real fraud data.
- **Native SHAP.** Every score ships with per-feature reason codes — required for
  analyst review and adverse-action/regulatory explainability.

## Real-time serving (the hand-off)

1. The streaming layer (Kafka Streams → Valkey) maintains the per-customer aggregates
   in `features.py` — that running state **is** the digital twin, kept current per event.
2. Your Spring scorer reads those aggregates from Valkey + computes the instant features
   (geo-velocity, new-device), assembles the 14-feature vector **in the exact order in
   `FEATURE_COLUMNS`**, and scores `fraud_model.onnx` via ONNX Runtime for Java.
3. Combine the model score with the deterministic rule gate from the earlier POC
   (rules = always-on floor + Resilience4j fallback if the model service degrades).
4. Emit ALLOW / CHALLENGE / BLOCK + SHAP reasons to a decisions topic.

**Train/serve feature parity is everything**: the aggregate you train on must be
computed identically online. A feature store (Feast, online store on Valkey) enforces
this with one definition for both paths.

```
