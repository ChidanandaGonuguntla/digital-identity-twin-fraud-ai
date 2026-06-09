# Recommended Next Sprint — Production Hardening

## Sprint goal
Move from deployment-ready artifacts to a bank-controlled production cutover with managed dependencies, identity federation, and operational runbooks.

## Week 1 — Platform integration
- Provision managed PostgreSQL (RDS/Azure Database) with `identity_twin` schema, Flyway baseline, and read replica for audit reporting.
- Connect Confluent Cloud / MSK / Redpanda with SASL_SSL, topic ACLs, and DLQ retention policies.
- Wire Keycloak or Okta OIDC into Spring Security (replace embedded JWT users in production).
- Store secrets in Vault / AWS Secrets Manager and reference via `secrets.existingSecret` in Helm.

## Week 2 — Observability and SRE (done)
- Prometheus scrape + recording/alert rules in `deploy/observability/prometheus/`.
- Grafana dashboards: Fraud Operations, Kafka/Model Drift, SLOs (`deploy/observability/grafana/`).
- OTEL collector + Tempo locally; `otel-collector-prod.yml` for Datadog/Azure Monitor.
- SLO catalog in `deploy/observability/SLOs.md` with p95 latency, availability, kafka lag, model drift.

## Week 3 — Resilience and scale
- Load test fraud decision API (k6/Gatling) with HPA validation under peak TPS.
- Enable PodDisruptionBudgets, topology spread constraints, and multi-AZ node pools.
- Validate ONNX model cold start, memory limits, and rollback drill (`POST /models/admin/rollback`).
- Run chaos tests: Kafka broker loss, DB failover, pod eviction.

## Week 4 — Compliance and go-live
- Complete penetration test and SOC-aligned audit log retention (decision + governance CSV exports).
- Finalize runbooks: deploy, rollback, secret rotation, incident response, model approval workflow.
- Execute blue/green or canary release with `helm upgrade` and traffic shift via Ingress weights.
- Sign off model risk committee checklist before enabling `APP_SECURITY_ENABLED=true` in production.

## Definition of done
- Staging and production namespaces deployed via GitHub Actions or Jenkins with approved image tags.
- All probes green, HPA scaling verified, managed DB and Kafka connected, IdP login working end-to-end.
- Monitoring alerts routed to on-call with documented escalation paths.
