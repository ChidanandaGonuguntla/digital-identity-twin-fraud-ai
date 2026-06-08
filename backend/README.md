# Digital Twin Identity — Fraud Prevention POC

A Spring Boot proof-of-concept that models each customer as a **behavioral digital twin**:
a virtual identity that stays continuously synchronized with the customer's real
transaction behavior. Live transactions are reconciled against the twin, and deviations
(reported vs. expected behavior) are surfaced as explainable fraud risk *before* the
transaction is approved.

## The idea

A digital twin keeps three things in play — the **reported state** (what just happened),
the **expected state** (what the twin's learned baseline predicts), and a **synchronization
loop** that folds trusted behavior back into the model. For fraud, the "asset" being
twinned is the customer's transactional identity. Account takeover and card fraud show up
as a sudden divergence between a live event and the twin.

## Fraud signals scored

Each signal contributes weighted points to a 0–100 risk score:

| Signal                   | What it catches                                                        | Default weight |
|--------------------------|------------------------------------------------------------------------|----------------|
| Amount anomaly (z-score) | Spend far outside the customer's normal range                          | 30             |
| Geo-velocity             | "Impossible travel" — two locations too far apart for the time elapsed | 40             |
| New device fingerprint   | Login/charge from an unrecognized device                               | 20             |
| Unusual hour             | Activity at an hour the customer never transacts                       | 15             |
| New merchant category    | First-ever spend in a category                                         | 15             |

Score → decision: `< 40` ALLOW · `40–69` CHALLENGE (step-up auth) · `>= 70` BLOCK.
All weights/thresholds live in `application.yml` and are hot-tunable without code changes.

## Design choices that matter for banking

- **Explainable, not black-box.** Every assessment returns human-readable `reasons` —
  auditable for analysts and regulators.
- **Twin isn't poisoned by fraud.** Blocked transactions are *not* synced into the
  baseline, so attacks never teach the model.
- **Cold-start safe.** New customers with too little history are observed, not punished,
  avoiding onboarding false positives.
- **Deterministic first tier.** This is the rules-based gate; an ML model or LLM-RAG
  narrative layer would sit *behind* it, never as the sole decision-maker.

## Architecture

```
TransactionEvent (REST / would be Kafka in prod)
        │
        ▼
FraudDetectionService  ──►  DeviationScoringEngine   (reported vs. expected → RiskAssessment)
        │                          ▲
        │                          │ reads baseline
        ▼                          │
TwinSynchronizationService ──► IdentityTwin / BehavioralProfile   (the synced model)
        │
        ▼
IdentityTwinRepository (in-memory; prod: Valkey hot state + Postgres + Timescale history)
```

## Run it

```bash
mvn spring-boot:run
```

### 1. Seed a baseline (known-good history)

```bash
curl -s -X POST http://localhost:8080/api/v1/transactions/seed \
  -H 'Content-Type: application/json' \
  -d '[
    {"customerId":"CUST-001","transactionId":"s1","amount":52,"merchantCategory":"GROCERY","deviceId":"dev-A","latitude":35.2271,"longitude":-80.8431,"timestamp":"2026-05-01T14:00:00Z"},
    {"customerId":"CUST-001","transactionId":"s2","amount":48,"merchantCategory":"GROCERY","deviceId":"dev-A","latitude":35.2271,"longitude":-80.8431,"timestamp":"2026-05-02T14:05:00Z"},
    {"customerId":"CUST-001","transactionId":"s3","amount":61,"merchantCategory":"GROCERY","deviceId":"dev-A","latitude":35.2271,"longitude":-80.8431,"timestamp":"2026-05-03T13:55:00Z"},
    {"customerId":"CUST-001","transactionId":"s4","amount":55,"merchantCategory":"GROCERY","deviceId":"dev-A","latitude":35.2271,"longitude":-80.8431,"timestamp":"2026-05-04T14:10:00Z"},
    {"customerId":"CUST-001","transactionId":"s5","amount":50,"merchantCategory":"GROCERY","deviceId":"dev-A","latitude":35.2271,"longitude":-80.8431,"timestamp":"2026-05-05T14:00:00Z"},
    {"customerId":"CUST-001","transactionId":"s6","amount":58,"merchantCategory":"GROCERY","deviceId":"dev-A","latitude":35.2271,"longitude":-80.8431,"timestamp":"2026-05-06T14:00:00Z"}
  ]'
```

### 2. Normal transaction → ALLOW

```bash
curl -s -X POST http://localhost:8080/api/v1/transactions/assess \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"CUST-001","transactionId":"t-ok","amount":54,"merchantCategory":"GROCERY","deviceId":"dev-A","latitude":35.2271,"longitude":-80.8431,"timestamp":"2026-05-07T14:00:00Z"}'
```

### 3. Fraud (impossible travel + new device + huge amount) → BLOCK

```bash
curl -s -X POST http://localhost:8080/api/v1/transactions/assess \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"CUST-001","transactionId":"t-fraud","amount":1450,"merchantCategory":"ELECTRONICS","deviceId":"dev-UNKNOWN","latitude":1.3521,"longitude":103.8198,"timestamp":"2026-05-07T14:20:00Z"}'
```

### 4. Inspect the twin

```bash
curl -s http://localhost:8080/api/v1/twins/CUST-001
```

## Productionizing this

- **Ingestion:** replace the REST `assess` entry point with a Kafka consumer on your
  auth/transaction stream; emit `RiskAssessment` to a decisions topic.
- **State:** Valkey/Redis for hot twin reads, PostgreSQL (JSONB) for durable snapshots,
  TimescaleDB for raw event history and offline retraining.
- **Resilience:** wrap downstream calls (geo lookup, ML scoring) in Resilience4j circuit
  breakers; the deterministic engine is the safe fallback.
- **Observability:** Datadog monitors on block rate, challenge rate, and score
  distribution drift per segment.
- **Later tiers:** add an ML anomaly model and an LLM-RAG layer that turns the `reasons`
  into an analyst-facing case narrative — behind the deterministic gate, never replacing it.

```
