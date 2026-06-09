# Fraud Platform Service Level Objectives

## SLO catalog

| SLO | SLI | Target | Window | Alert threshold |
|---|---|---|---|---|
| Decision latency p95 | `slo:fraud_decision_latency_p95:5m` | <= 300 ms | 30 days | > 300 ms for 5 min |
| Decision latency p99 | `slo:fraud_decision_latency_p99:5m` | <= 750 ms | 30 days | > 750 ms for 5 min |
| API availability | `slo:fraud_api_availability:5m` | >= 99.9% | 30 days | < 99.9% for 5 min |
| Service up | `slo:fraud_service_up:1m` | 100% | 30 days | down 2 min |
| Kafka consumer lag | `slo:fraud_kafka_lag:1m` | < 1,000 messages | 7 days | > 1,000 for 10 min |
| Model drift | `slo:fraud_model_drift:1m` | < 0.35 | 7 days | > 0.35 for 15 min |
| Model drift MTTR | `fraud_model_drift_alert == 1` | acknowledge < 30 min | incident | alert active 15 min |

## Error budget

Monthly availability error budget at 99.9%: **43.2 minutes** downtime equivalent.

Burn rate alerts:
- Fast burn: `slo:fraud_error_budget_burn:5m > 0.01` for 5 minutes
- Slow burn: `slo:fraud_error_budget_burn:5m > 0.001` for 1 hour

## Escalation

| Severity | Examples | Route |
|---|---|---|
| critical | Service down, p99 latency, audit write failures | fraud-platform on-call |
| warning | p95 latency, kafka lag, model drift | fraud-platform + model-risk |

## Dashboards

- **Fraud Operations** — throughput, block rate, latency, availability
- **Kafka Lag and Model Drift** — pipeline lag, ML drift, twin drift
- **Fraud Platform SLOs** — SLO compliance and error budget burn

## Trace correlation

Traces export via OTLP to the collector (`otel-collector:4318`) and land in Tempo. Grafana links Tempo traces to Prometheus metrics using the shared `service` label.

## Local stack

```bash
OTEL_METRICS_EXPORT_ENABLED=true OTEL_TRACES_SAMPLER_PROBABILITY=0.1 \
  docker compose --profile local-infra --profile monitoring up --build
```

| Service | URL |
|---|---|
| Grafana | http://localhost:3001 (admin / admin) |
| Prometheus | http://localhost:9090 |
| Alertmanager | http://localhost:9093 |
| Tempo | http://localhost:3200 |
| OTLP HTTP | http://localhost:4318 |
