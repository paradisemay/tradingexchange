# Plan: Go quotes-collector

## Этап 1 — Скелет + чтение драйвера (текущий)
- [x] proto/quotes.proto — контракт QuoteTick
- [x] go.mod, config, model.Quote, .env.example
- [x] internal/driver — бинарное чтение cQuote → model.Quote + timestamp
- [x] internal/driver — unit-тесты (cStringToGo, binary parse, channel drop)
- [x] internal/publisher + internal/batcher — интерфейсы + log-моки
- [x] internal/pipeline/fanout — fan-out из канала в два сервиса
- [x] cmd/quotes-collector/main.go — сборка, signal handling, graceful shutdown

## Этап 2 — Redis publisher (задача ttemmaaa/amsterdam121)
- [ ] internal/publisher/redis.go — XADD stream:quotes:v1, payload=protobuf
- [ ] protobuf-кодирование QuoteTick
- [ ] retry + exponential backoff
- [ ] integration test через testcontainers-go

## Этап 3 — ClickHouse batcher (задача amsterdam121)
- [x] internal/batcher/clickhouse.go — батч ≤10k / flush 200ms
- [x] graceful flush на shutdown
- [x] integration test через testcontainers-go

## Этап 4 — Docker
- [ ] Dockerfile (multi-stage)
- [ ] docker-compose.yml (redis + clickhouse + collector)
- [x] отдельный ClickHouse compose-модуль в `../clickhouse`
