# Telemetry Implementation Report

## What was done

The telemetry module was turned from a documentation-only folder into a working observability stack for the trading exchange project.

Implemented in this folder:

- OpenTelemetry Collector with OTLP HTTP and OTLP gRPC receivers.
- Prometheus for metrics storage and scraping.
- Loki for log storage.
- Tempo for distributed traces.
- Grafana provisioning for datasources and a starter dashboard.
- Redis exporter and PostgreSQL exporter in the same Compose topology.
- Promtail for file-based log shipping.
- A smoke script for checking the health of the telemetry stack.

The stack is designed to work without changing runtime code in other modules. Ktor already knows the collector endpoint, and Go can use the same collector network entry points.

## What is collected where

### OpenTelemetry Collector

Collector is the entry point for application telemetry.

- Receives traces and metrics from Ktor over OTLP.
- Receives OTLP data from Go-compatible clients over gRPC or HTTP.
- Forwards traces to Tempo.
- Exposes a Prometheus scrape endpoint for OTEL-exported metrics.

### Prometheus

Prometheus stores infrastructure and application metrics.

Collected here:

- `redis_up`
- `redis_memory_used_bytes`
- `redis_connected_clients`
- `redis_instantaneous_ops_per_sec`
- `redis_commands_processed_total`
- `redis_keyspace_hits_total`
- `redis_keyspace_misses_total`
- `redis_evicted_keys_total`
- `pg_up`
- `pg_stat_database_numbackends`
- `pg_stat_database_xact_commit`
- `pg_stat_database_xact_rollback`
- `pg_locks_count`
- `pg_stat_activity_count`
- `pg_database_size_bytes`

Planned application metrics from Ktor are also expected here once the backend exports them:

- `http_requests_total`
- `http_request_duration`
- `ws_active_connections`
- `redis_reconnect_count`
- `db_transaction_duration`

### Tempo

Tempo stores distributed traces.

Expected trace flow:

- HTTP request enters Ktor.
- Ktor emits OTLP trace data to the collector.
- Collector forwards traces to Tempo.
- Tempo is then used to inspect request paths across Ktor, Redis interaction, and PostgreSQL activity.

### Loki

Loki stores logs.

Current log pipeline is staged through Promtail and a mounted log path:

- `./logs` in the telemetry folder
- `/var/log/telemetry` inside Promtail

If Ktor writes structured JSON logs to file, those logs are shipped to Loki and can be searched by `traceId`.

### Grafana

Grafana is the visualization layer.

Provisioned today:

- Prometheus datasource
- Loki datasource
- Tempo datasource
- starter dashboard for the telemetry overview

## Metric map by source

### Redis exporter

Source: `redis-exporter`

Location: `http://localhost:9121/metrics`

Metrics:

- health and availability (`redis_up`)
- memory usage (`redis_memory_used_bytes`)
- client load (`redis_connected_clients`)
- command throughput (`redis_instantaneous_ops_per_sec`, `redis_commands_processed_total`)
- cache usefulness (`redis_keyspace_hits_total`, `redis_keyspace_misses_total`)
- memory pressure (`redis_evicted_keys_total`)

### PostgreSQL exporter

Source: `postgres-exporter`

Location: `http://localhost:9187/metrics`

Metrics:

- health and availability (`pg_up`)
- active connections (`pg_stat_database_numbackends`)
- transaction activity (`pg_stat_database_xact_commit`, `pg_stat_database_xact_rollback`)
- lock pressure (`pg_locks_count`)
- session activity (`pg_stat_activity_count`)
- database size (`pg_database_size_bytes`)

### Ktor OTEL metrics

Source: Ktor OpenTelemetry instrumentation

Transport: OTLP HTTP to `http://otel-collector:4318`

Metrics expected from the backend:

- request rate
- request duration
- WebSocket session count
- Redis reconnect count
- transaction duration

## Current follow-up items

- Ktor still needs to actually emit the application metrics listed above.
- Log export is staged through file-based shipping; if Ktor remains stdout-only, it needs a small follow-up to mirror logs to a file or export logs directly over OTLP.
- Go and Ktor should eventually align on the same OTLP transport preference, but the collector already accepts both HTTP and gRPC.
