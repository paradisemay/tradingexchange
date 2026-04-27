# Plan: ClickHouse + Go batching module

## Context
- Source requirements: `Мобилки.txt`, `ТЗ.md`, and `C:\Users\yarik\Downloads\Мобилки (1).pdf`.
- Scope restriction: add a new `clickhouse/` module and update only the Go collector code needed for ClickHouse batching.
- Security rule: no secrets in code; every host, port, user, password, timeout, and batch setting is controlled through environment variables and `.env.example`.
- Documentation rule: every architectural/library choice is documented in wiki/ADR files.

## Tasks
- [x] Read root project requirements and existing Go/Ktor/Redis contracts.
- [x] Add `clickhouse/` Docker Compose module with persistent storage, init SQL, healthcheck, `.env.example`, README, and smoke scripts.
- [x] Add mock data generation and sample Ktor-facing query scripts for raw points, candles, summary, and latest price.
- [x] Implement Go ClickHouse batcher: native TCP, max batch size, timed flush, retry with backoff, graceful shutdown flush.
- [x] Add Go config validation for ClickHouse environment variables.
- [x] Add unit tests for batching behavior.
- [x] Add ClickHouse integration test with testcontainers-go.
- [x] Update Go docs/wiki and create ADR records for ClickHouse MergeTree, native protocol, and batching decisions.
- [ ] Run `gofmt`, `go test`, and integration tests where Docker is available.
- [ ] Make small Conventional Commits.
