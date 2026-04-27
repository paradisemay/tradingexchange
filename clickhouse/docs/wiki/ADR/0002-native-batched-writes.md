# ADR 0002: Native Protocol and Batched Writes

## Status

Accepted

## Context

The Go collector receives frequent quote ticks from the C driver. Sending each tick as a separate insert would create unnecessary overhead and increase ClickHouse part pressure.

## Decision

The Go collector writes through `github.com/ClickHouse/clickhouse-go/v2` over the native TCP protocol. It buffers quotes and flushes when either condition is met:

- buffer reaches `CLICKHOUSE_BATCH_SIZE`, default `10000`;
- `CLICKHOUSE_FLUSH_INTERVAL_MS` elapses, default `200`.

Temporary insert failures are retried with exponential backoff. On graceful shutdown, the remaining buffer is flushed before closing the ClickHouse connection.

## Alternatives

- HTTP inserts: easier to debug manually, but less natural for high-rate Go batching and typed prepared batches.
- Single-row inserts: simplest code, but violates the SRS and harms ClickHouse write performance.
- Kafka between Go and ClickHouse: more durable and scalable, but out of scope for this project stage.

## Consequences

Go owns write backpressure and retry behavior. ClickHouse receives fewer, larger inserts and keeps table part counts under control.

