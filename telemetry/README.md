# Telemetry Module

This folder owns the observability stack for the trading exchange project.

## What is included

- OpenTelemetry Collector for OTLP traces, metrics, and logs.
- Prometheus for metrics storage and dashboards.
- Loki for log storage.
- Tempo for distributed traces.
- Grafana for visualization.
- Redis and PostgreSQL exporters for infrastructure metrics.
- Promtail as a log shipper for file-based log ingestion.
- A starter Grafana dashboard is provisioned from `grafana/dashboards/overview.json`.

## Current integration contract

- Ktor sends OTLP to `http://otel-collector:4318`.
- Go should be able to use the collector on both `4317` and `4318` until the transport choice is fully aligned.
- Redis exporter defaults to `redis:6379`.
- PostgreSQL exporter defaults to `postgresql://broker_app:change-me@postgres:5432/broker?sslmode=disable`.
- Tempo listens for OTLP from the collector on the internal telemetry network; only the query UI is published to the host.
- Log ingestion is staged through Promtail and a mounted file path (`./logs` → `/var/log/telemetry`). If Ktor keeps writing only to stdout, the Ktor module will need a small follow-up to mirror logs to a file or to enable OTLP logs.

## Start locally

```bash
cp .env.example .env
docker compose up -d

bash scripts/smoke.sh
```

## Smoke checks

The smoke script checks that the stack endpoints answer and that the exporter metrics pages are reachable.

## Expected ports

- Grafana: `3000`
- Prometheus: `9090`
- Loki: `3100`
- Tempo: `3200`
- OTLP gRPC: `4317`
- OTLP HTTP: `4318`
- Collector Prometheus endpoint: `8889`
- Redis exporter: `9121`
- PostgreSQL exporter: `9187`

## Notes

This first slice intentionally focuses on infrastructure and contract alignment. Runtime changes in Ktor or Go are not part of this folder and will be called out separately if they become necessary.

## Docs

- [Implementation report](docs/implementation-report.md)
- [Source SRS](docs/srs)
