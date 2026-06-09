# Digital Twin Identity — WebSocket Decision Stream

The live wire between the Spring fraud backend and the enterprise React console. Every
scored transaction is broadcast on STOMP topic `/topic/decisions` after the canonical
fraud decision pipeline completes.

## Canonical decision path

All ingress (REST and Kafka) uses a single entry point:

```
FraudDecisionController / TransactionEventKafkaConsumer
        ↓
FraudDecisionApplicationService.evaluate()
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
DecisionPublisher
        ↓
FraudDecisionResponse
```

Legacy MVP services (`FraudDetectionService`, `FraudEvolutionService`) are removed.
Do not add parallel scoring entry points.

## WebSocket stack

| Component                    | Package                    | Role                                              |
|------------------------------|----------------------------|---------------------------------------------------|
| `WebSocketConfig`            | `infrastructure.config`    | Registers STOMP endpoint `/ws/decisions`          |
| `WebSocketDecisionPublisher` | `infrastructure.websocket` | Publishes to `/topic/decisions`                   |
| `CompositeDecisionPublisher` | `infrastructure.kafka`     | Fan-out to WebSocket + Kafka                      |
| `DecisionPublisher`          | `application.service`      | Port invoked by `FraudDecisionApplicationService` |
| `DecisionEventFactory`       | `application.service`      | Builds wire payload from assessment               |

## End to end

```bash
mvn spring-boot:run
.\scripts\seed-baseline.ps1
.\scripts\run-fraud-demo.ps1 -SkipSeed
```

Frontend `.env`:

```env
VITE_API_BASE_URL=http://localhost:9997/api/v1
VITE_USE_MOCK=false
```

| Item      | Value                                   |
|-----------|-----------------------------------------|
| Endpoint  | `ws://localhost:9997/ws/decisions`      |
| Subscribe | `/topic/decisions`                      |
| Protocol  | STOMP over WebSocket (`@stomp/stompjs`) |

## Message shape

```json
{
  "assessmentId": "uuid",
  "transactionId": "TXN-...",
  "customerId": "CUST-001007",
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

## Security

When `APP_SECURITY_ENABLED=true`:

- REST APIs require `Authorization: Bearer <token>` from `POST /api/v1/auth/login`
- WebSocket connects with `ws://host:9997/ws/decisions?token=<accessToken>`
- Allowed origins come from `APP_CORS_ALLOWED_ORIGINS` (REST + WebSocket)

## Production notes

- Set `APP_SECURITY_ENABLED=true` and rotate `APP_JWT_SECRET`
- Restrict `APP_CORS_ALLOWED_ORIGINS` to your console origin
- Kafka outbound topic `dti.fraud-decisions` is published when `fraud.kafka.enabled=true`
- Clients should re-fetch recent audit rows on reconnect for replay
