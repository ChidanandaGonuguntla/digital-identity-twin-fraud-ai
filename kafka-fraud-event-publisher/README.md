# Kafka Fraud Event Publisher for Digital Identity Twin

This project publishes a synthetic 100,000-event fraud dataset into Kafka for your Spring Boot Digital Identity Twin module to consume.

## Dataset

Files included:

- `data/fraud-events-100k.jsonl` — 100,000 transaction events.
- `data/seed-known-good-10k.jsonl` — 10,000 normal baseline events for twin bootstrapping.
- `data/scenario-summary.json` — scenario distribution.

Covered scenarios:

- NORMAL_BASELINE
- HIGH_AMOUNT
- NEW_DEVICE
- NEW_COUNTRY
- IMPOSSIBLE_TRAVEL
- UNUSUAL_HOUR
- HIGH_RISK_CATEGORY
- CATEGORY_VELOCITY
- ML_COMPOSITE_RISK
- ACCOUNT_TAKEOVER_PATTERN

Each event matches the enterprise `TransactionEvent` contract:

```json
{
  "customerId": "CUST-000001",
  "transactionId": "TXN-000000001",
  "amount": 1250.75,
  "currency": "USD",
  "merchantCategory": "electronics",
  "merchantName": "Best Buy",
  "merchantId": "MER-7788",
  "deviceId": "DEV-000001-MOBILE",
  "ipAddress": "10.1.2.3",
  "userAgent": "CitizensMobile/7.4 iOS",
  "channel": "MOBILE",
  "paymentInstrumentId": "CARD-4455",
  "latitude": 35.2271,
  "longitude": -80.8431,
  "countryCode": "US",
  "city": "Charlotte",
  "timestamp": "2026-06-08T12:00:00Z",
  "metadata": {
    "scenario": "HIGH_AMOUNT",
    "riskLabel": "MEDIUM",
    "expectedDecision": "CHALLENGE"
  }
}
```

## Start Kafka locally

```bash
docker compose -f docker-compose.kafka.yml up -d
```

## Create topic

If you have Kafka CLI installed:

```bash
./scripts/create-topic.sh
```

Topic name defaults to:

```text
dti.transaction-events
```

## Publish seed transactions first

This gives your Digital Twin a known-good baseline before fraud testing.

```bash
./scripts/publish-seed-10k.sh
```

## Publish full 100k dataset

```bash
./scripts/publish-100k.sh
```

## Environment variables

```bash
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export DTI_TRANSACTION_TOPIC=dti.transaction-events
export DTI_DATASET_PATH=data/fraud-events-100k.jsonl
export DTI_PUBLISH_RATE=1000
export DTI_MAX_RECORDS=0
export DTI_PUBLISH_ON_STARTUP=true
```

## Your Digital Twin consumer should listen to

```yaml
spring:
  kafka:
    consumer:
      group-id: digital-twin-fraud-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.citizens.digital.twin.*,com.citizens.digital.twin.publisher.*
        spring.json.value.default.type: com.citizens.digital.twin.domain.event.TransactionEvent

digital-twin:
  kafka:
    transaction-events-topic: dti.transaction-events
```

Sample listener:

```java
@KafkaListener(topics = "${digital-twin.kafka.transaction-events-topic}", groupId = "digital-twin-fraud-service")
public void onTransactionEvent(TransactionEvent event) {
  fraudDecisionApplicationService.evaluate(event);
}
```

If your current event package is `com.citizens.digital.twin.model.TransactionEvent`, set `spring.json.value.default.type` to that instead.
