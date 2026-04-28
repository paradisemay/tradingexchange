# Telemetry Module Plan

## What I verified

- Ktor already has OpenTelemetry SDK wiring and points to `http://otel-collector:4318` by default.
- Ktor currently keeps OTel export disabled unless the environment switches `OTEL_*_EXPORTER` to `otlp`.
- Go docs still mention OTLP gRPC on `4317`, so the collector should support both HTTP and gRPC receivers.
- Redis and PostgreSQL are already exported through `redis-exporter` and `postgres-exporter` in the surrounding docs.

## Goal

Build the telemetry stack inside this folder only, without touching runtime code in other modules, so that the stack can receive:

- traces and metrics from Ktor through OpenTelemetry;
- metrics from Redis and PostgreSQL exporters;
- logs from Ktor through a log pipeline;
- everything inside one Docker Compose topology.

## Implementation plan

1. Create the telemetry infrastructure scaffold: `docker-compose.yml`, collector config, Prometheus config, Loki, Tempo, Grafana, and Promtail.
2. Expose OTLP HTTP and gRPC on the collector so both Ktor and Go contracts can be supported without a code change outside telemetry.
3. Configure Prometheus scrape jobs for `redis-exporter` and `postgres-exporter`.
4. Configure Loki ingestion for Ktor JSON logs through Promtail.
5. Add Grafana provisioning for datasources and a starter dashboard set.
6. Add smoke scripts and runbook documentation for local verification.
7. Validate the whole stack against the documented contracts and note any cross-module follow-ups if a neighbor still needs a small env or endpoint alignment.

## Current risks

- The telemetry folder is currently documentation-only, so all runtime files need to be created from scratch.
- The Ktor side already emits some OpenTelemetry signals, but export is off by default, so the collector must be ready before end-to-end verification.
- The Go module and Ktor module disagree on OTLP transport port in their docs, so receiver compatibility matters.
