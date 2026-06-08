# Digital Twin Identity — WebSocket Decision Stream

The live wire between the Spring fraud backend and the SENTINEL console. It broadcasts
every decided transaction to `/ws/decisions` in the exact JSON shape the console's
WebSocket client consumes, so the dashboard, live feed, and notifications light up from
real backend traffic.

## Files (drop into the existing POC)

Place under `src/main/java/com/citizens/dti/`:

```
web/
  DecisionEvent.java          wire contract (TransactionEvent + RiskAssessment merged)
  SignalContribution.java     one signal {name, contribution}
  DecisionEventFactory.java   builds the wire event; derives structured signals from reasons
  DecisionStreamHandler.java  the broadcaster (thread-safe session fan-out)
  WebSocketConfig.java        registers /ws/decisions
  DecisionPublisher.java      seam the domain service calls
  DemoDecisionEmitter.java    OPTIONAL — synthetic stream under the 'demo' profile
  KafkaDecisionConsumer.java  OPTIONAL — production decisions-topic bridge ('kafka' profile)
service/
  FraudDetectionService.java  REPLACES the turn-1 version (adds the publish call)
```

## 1. Add the dependency

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

## 2. Wire it in

The only domain change is in `FraudDetectionService.assess(...)` — it now takes a
`DecisionPublisher` and calls `publish(event, assessment)` after scoring. The replacement
file is included; everything else is additive.

That's it. Every call to `POST /api/v1/transactions/assess` now also broadcasts to all
connected consoles.

## 3. See it end to end

```bash
# backend — synthetic stream, no Kafka needed
mvn spring-boot:run -Dspring-boot.run.profiles=demo

# frontend — point at the backend instead of its own simulator
#   .env:  VITE_USE_MOCK=false
npm run dev
```

The console's status pill flips to **BACKEND · Live** and transactions stream in from
Spring. Drop the `demo` profile and the same stream is driven by real `/assess` calls.

## Message shape (what gets broadcast)

```jsonc
{
  "transactionId": "TXN-...", "customerId": "CUST-01007",
  "amount": 1450.0, "merchantCategory": "electronics", "deviceId": "dev-unknown-x",
  "latitude": 1.3521, "longitude": 103.8198, "timestamp": 1733600000000,
  "riskScore": 84.0, "decision": "BLOCK", "coldStart": false,
  "signals": [ { "name": "geo_velocity", "contribution": 42.0 } ],
  "reasons": [ "Impossible travel detected ...", "Unrecognized device fingerprint" ]
}
```

`signals` are derived from the engine's reason codes and the composite score (see
`DecisionEventFactory`). When you have the scoring engine emit signal contributions
directly, swap `deriveSignals` for a pass-through.

## Production path (Kafka)

In production the assessment is produced by your stream processor and written to a Kafka
decisions topic. `KafkaDecisionConsumer` (profile `kafka`) bridges that topic to the same
WebSocket fan-out — add `spring-kafka`, configure the JSON deserializer (see the class
Javadoc), and the REST and Kafka paths coexist.

> **Compile note:** `KafkaDecisionConsumer.java` imports `spring-kafka`. Add that
> dependency **before** including the file, or omit the file — otherwise the build fails
> on the missing import. All other files compile with just `spring-boot-starter-websocket`.

## Hardening notes

- `WebSocketConfig` allows all origins for local dev — restrict to your console origin
  in production and add an auth handshake interceptor (e.g. validate a JWT).
- For STOMP instead of raw WebSocket, register a STOMP broker and publish to
  `/topic/decisions`; the console swap is documented in its own README.
- The broadcaster is best-effort (drops dead sessions). For guaranteed delivery or
  replay, have clients re-fetch recent decisions over REST on reconnect.

```
