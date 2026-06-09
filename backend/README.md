# Digital Twin Identity — Fraud Prevention Backend

Spring Boot backend for the Digital Identity Twin fraud platform. Each customer has a
behavioral twin that learns from trusted transactions. Live events are scored by rule,
twin-deviation, and ML engines, then published over STOMP WebSocket for the React console.

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 14+ with database `digital_twin_db`
- Default credentials: `postgres` / `admin` (override via env vars below)

## Run

```bash
mvn spring-boot:run
```

Server starts on **port 9997**.

Flyway runs automatically on startup and applies:

- `V1__identity_twin_schema.sql` — platform tables (`customer_twin`, `ml_fraud_score`, …)
- `V2__jpa_entity_tables.sql` — JPA tables used by the app (`identity_twins`, `fraud_decision_audit`)

### Environment overrides

```bash
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/digital_twin_db?currentSchema=identity_twin
set SPRING_DATASOURCE_USERNAME=postgres
set SPRING_DATASOURCE_PASSWORD=admin
```

## Quick start (seed + demo)

From the `backend` folder:

```powershell
# 1. Start the backend
mvn spring-boot:run

# 2. In another terminal — build baseline twin history (6 ALLOW grocery txns)
.\scripts\seed-baseline.ps1

# 3. Run normal + fraud scenarios (seeds first unless -SkipSeed is passed)
.\scripts\run-fraud-demo.ps1
```

Expected demo output:

- `txn-normal` → **ALLOW** (matches learned grocery baseline)
- `txn-fraud` → **BLOCK** (impossible travel + new device + high amount)

Each scored transaction is:

- Persisted to `fraud_decision_audit`
- Broadcast on STOMP topic `/topic/decisions`
- Synced into the twin when decision is **ALLOW**

## API reference

| Method | Path                                 | Description                             |
|--------|--------------------------------------|-----------------------------------------|
| `POST` | `/api/v1/fraud/decisions`            | Score a transaction and return decision |
| `POST` | `/api/v1/fraud/score`                | Alias of `/decisions`                   |
| `GET`  | `/api/v1/audit/decisions/recent`     | Recent audit records                    |
| `GET`  | `/api/v1/twins/{customerId}`         | Twin summary for a customer             |
| `GET`  | `/api/v1/models/fraud-risk/metadata` | ML model metadata                       |
| `GET`  | `/api/v1/models/fraud-risk/health`   | Model health snapshot                   |

### Score a transaction

```bash
curl -s -X POST http://localhost:9997/api/v1/fraud/decisions \
  -H "Content-Type: application/json" \
  -d "{\"customerId\":\"CUST-001\",\"transactionId\":\"TXN-100\",\"amount\":54,\"currency\":\"USD\",\"merchantCategory\":\"GROCERY\",\"deviceId\":\"dev-A\",\"channel\":\"MOBILE\",\"latitude\":35.2271,\"longitude\":-80.8431,\"countryCode\":\"US\",\"timestamp\":\"2026-05-07T14:00:00Z\"}"
```

### Inspect twin

```bash
curl -s http://localhost:9997/api/v1/twins/CUST-001
```

### Recent decisions

```bash
curl -s http://localhost:9997/api/v1/audit/decisions/recent
```

## WebSocket (STOMP)

| Item      | Value                              |
|-----------|------------------------------------|
| Endpoint  | `ws://localhost:9997/ws/decisions` |
| Subscribe | `/topic/decisions`                 |
| Protocol  | STOMP over WebSocket               |

Message shape (published after each `/fraud/decisions` call):

```json
{
  "assessmentId": "uuid",
  "transactionId": "TXN-...",
  "customerId": "CUST-001",
  "amount": 1450.0,
  "merchantCategory": "ELECTRONICS",
  "deviceId": "dev-UNKNOWN",
  "latitude": 1.3521,
  "longitude": 103.8198,
  "eventTimeEpochMs": 1733600000000,
  "riskScore": 84.0,
  "decision": "BLOCK",
  "coldStart": false,
  "signals": [{ "name": "geo_velocity", "contribution": 42.0 }],
  "reasons": ["Impossible travel detected", "Unrecognized device fingerprint"]
}
```

See `WEBSOCKET_INTEGRATION.md` for frontend wiring notes.

## React console integration

In the UI project `.env`:

```env
VITE_API_BASE_URL=/api/v1
VITE_USE_MOCK=false
```

Vite proxies `/api` and `/ws` to `http://localhost:9997`. The console uses STOMP
(`@stomp/stompjs`) and subscribes to `/topic/decisions`.

```bash
cd ../digital-twin-enterprise-react-ui-rich
npm install
npm run dev
```

Open `http://localhost:5173` — status pill should show **WS LIVE** after the backend is up.

## ML model (XGBoost → ONNX)

Production scoring uses an embedded ONNX model loaded at startup. The `MlFraudModelService`
interface allows swapping implementations without touching the orchestrator.

| Component                        | Role                                         |
|----------------------------------|----------------------------------------------|
| `FraudFeatureEngineeringService` | Builds the 10-feature vector per transaction |
| `OnnxFraudModelService`          | ONNX Runtime inference                       |
| `RoutedMlFraudModelService`      | Routes ONNX vs heuristic fallback            |
| `ModelRegistryService`           | Load, reload, rollback active version        |
| `ModelDriftMonitorService`       | Drift score vs model baseline                |
| `ModelMetricsRecorderService`    | Persists feature vectors + latency           |

### Train and export

```powershell
cd backend/ml
py -3.13 -m pip install -r requirements.txt
py -3.13 train_xgboost_onnx.py
```

Artifacts are written to `src/main/resources/models/`:

- `fraud-risk-v1.0.0.onnx` — active model
- `fraud-risk-v0.9.0.onnx` — rollback model
- `*.metadata.json` — version, metrics, feature order, drift baseline

### Runtime configuration

```yaml
dti.ml:
  provider: onnx          # onnx | heuristic
  active-version: fraud-risk-v1.0.0
  rollback-version: fraud-risk-v0.9.0
  drift-baseline-score: 0.15
  drift-alert-threshold: 0.35
```

Or via env: `DTI_ML_PROVIDER=heuristic` to force the embedded heuristic baseline.

### Model admin APIs

| Method | Path                            | Description                                  |
|--------|---------------------------------|----------------------------------------------|
| `GET`  | `/api/v1/models/admin/status`   | Provider, active version, ONNX loaded, drift |
| `GET`  | `/api/v1/models/admin/versions` | Configured model versions                    |
| `POST` | `/api/v1/models/admin/reload`   | Reload active ONNX artifact                  |
| `POST` | `/api/v1/models/admin/rollback` | Switch to rollback version                   |

Each fraud decision includes `modelVersion`, `modelType`, and the full feature vector in
`model_metrics.feature_snapshot` for observability and drift analysis.

## Scoring thresholds

Configured in `application.yml` under `dti.scoring`:

| Setting               | Default | Effect                                             |
|-----------------------|---------|----------------------------------------------------|
| `min-history`         | 3       | Minimum trusted txns before full deviation scoring |
| `challenge-threshold` | 40      | Score at or above → CHALLENGE                      |
| `block-threshold`     | 70      | Score at or above → BLOCK                          |

The orchestrator also applies weighted fusion: rule 35% + twin 35% + ML 30%.

## Architecture

Single enterprise decision path — REST (`FraudDecisionController`) and Kafka
(`TransactionEventKafkaConsumer`) both call `FraudDecisionApplicationService.evaluate()`.
Legacy MVP services (`FraudDetectionService`, `FraudEvolutionService`) are not used.

```
FraudDecisionController / TransactionEventKafkaConsumer
        ↓
FraudDecisionApplicationService
        ↓
IdentityTwinService
        ↓
RuleRiskEngine
        ↓
TwinDeviationScoringService
        ↓
MlFraudModelService
        ↓
RiskDecisionOrchestrator
        ↓
FraudDecisionAuditService
        ↓
TwinSynchronizationService (ALLOW only)
        ↓
DecisionPublisher → WebSocket + Kafka
        ↓
FraudDecisionResponse
```

### Enterprise readiness

| Area                                                                            | Status                                             |
|---------------------------------------------------------------------------------|----------------------------------------------------|
| Package layout (`api` / `application` / `domain` / `infrastructure` / `shared`) | Done                                               |
| Single fraud decision path                                                      | Done                                               |
| Flyway migrations (`V1`, `V2`)                                                  | Done                                               |
| Request validation (`@Valid` on `FraudDecisionRequest`)                         | Done                                               |
| Standardized error envelope (`ApiErrorResponse`)                                | Done                                               |
| Integration tests (Testcontainers PostgreSQL)                                   | Done — `mvn test`                                  |
| JWT auth (optional, `APP_SECURITY_ENABLED=true`)                                | Done                                               |
| Env-driven CORS (`APP_CORS_ALLOWED_ORIGINS`)                                    | Done                                               |
| WebSocket JWT handshake (`?token=`)                                             | Done                                               |
| Integration tests (audit, model monitoring, security, step-up)                  | Done                                               |
| Flyway V3–V7 (step_up, model_metrics, drift, case_notes, JSONB audit)           | Done                                               |
| `ddl-auto: validate`                                                            | Done                                               |
| Kafka multi-topic publish (decisions, audit, step-up, drift)                    | Done                                               |
| Kafka consumer DLT (`dti.transaction-events.dlt`)                               | Done                                               |
| Step-up workflow (create/approve/reject/expire)                                 | Done                                               |
| ONNX ML inference (XGBoost → ONNX, reload/rollback)                             | Done                                               |
| Feature vector logging + drift monitoring                                       | Done                                               |
| Full API success envelope wrapper                                               | Pending (success DTOs remain direct for UI compat) |

## Troubleshooting

| Symptom                                  | Likely cause                               | Fix                                                                      |
|------------------------------------------|--------------------------------------------|--------------------------------------------------------------------------|
| `identity_twins does not exist`          | Flyway V2 not applied                      | Restart backend — V2 runs after baseline                                 |
| `no schema history table` / `baseline()` | Schema exists but Flyway never ran         | `baseline-on-migrate: true` is set in `application.yml`; restart backend |
| `fraud_decision_audit does not exist`    | Flyway V2 not applied                      | Same as above                                                            |
| Audit returns `[]`                       | No transactions scored yet                 | Run `.\scripts\seed-baseline.ps1`                                        |
| WebSocket connects but no events         | No `/fraud/decisions` calls                | Run seed or demo scripts                                                 |
| UI shows mock data                       | `VITE_USE_MOCK=true` or backend down       | Set `VITE_USE_MOCK=false`, restart UI                                    |
| POST returns 500 JDBC error              | PostgreSQL not running / wrong credentials | Start Postgres, check `application.yml`                                  |

## Kafka ingestion

Kafka is **off by default**. Enable it when you want transaction events consumed from a topic
instead of (or in addition to) REST.

```yaml
# application.yml or env
fraud:
  kafka:
    enabled: true
    transaction-topic: dti.transaction-events
    decision-topic: dti.fraud-decisions
```

Or at runtime:

```bash
set FRAUD_KAFKA_ENABLED=true
mvn spring-boot:run
```

| Direction    | Topic                    | Payload type                | Handler                                                                        |
|--------------|--------------------------|-----------------------------|--------------------------------------------------------------------------------|
| **Inbound**  | `dti.transaction-events` | `FraudDecisionRequest` JSON | `TransactionEventKafkaConsumer` → `FraudDecisionApplicationService.evaluate()` |
| **Outbound** | `dti.fraud-decisions`    | `DecisionEvent` JSON        | `FraudDecisionKafkaPublisher` (after each score)                               |

Inbound message shape (same as REST body):

```json
{
  "customerId": "CUST-001",
  "transactionId": "TXN-KAFKA-1",
  "amount": 54,
  "currency": "USD",
  "merchantCategory": "GROCERY",
  "deviceId": "dev-A",
  "channel": "MOBILE",
  "latitude": 35.2271,
  "longitude": -80.8431,
  "timestamp": "2026-05-07T14:00:00Z"
}
```

Each consumed message is scored, audited, synced to the twin (if ALLOW), published to
STOMP `/topic/decisions`, and written to `dti.fraud-decisions` when Kafka is enabled.

Requires Kafka at `spring.kafka.bootstrap-servers` (default `localhost:9092`).

## Security and CORS

Auth is **off by default** for local development. Enable JWT protection:

```bash
set APP_SECURITY_ENABLED=true
set APP_JWT_SECRET=replace-with-32-plus-character-production-secret
mvn spring-boot:run
```

| Variable                   | Default                    | Purpose                                                     |
|----------------------------|----------------------------|-------------------------------------------------------------|
| `APP_SECURITY_ENABLED`     | `false`                    | Require Bearer JWT on `/api/v1/**` except `/api/v1/auth/**` |
| `APP_JWT_SECRET`           | dev secret                 | HMAC signing key (use 32+ chars in production)              |
| `APP_CORS_ALLOWED_ORIGINS` | `localhost:3000,5173,5174` | Comma-separated REST + WebSocket origins                    |

Login:

```bash
curl -s -X POST http://localhost:9997/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"analyst@citizens.com\",\"password\":\"password\"}"
```

Use `Authorization: Bearer <accessToken>` on API calls. For WebSocket, connect to
`ws://localhost:9997/ws/decisions?token=<accessToken>` when security is enabled.

Demo users are configured in `application.yml` under `app.security.users`.

## Production notes

- REST and Kafka ingest can run together when `fraud.kafka.enabled=true`
- Set `APP_SECURITY_ENABLED=true` and rotate `APP_JWT_SECRET` before production
- Restrict `APP_CORS_ALLOWED_ORIGINS` to your console origin
- Valkey/Redis for hot twin reads; TimescaleDB for event history
