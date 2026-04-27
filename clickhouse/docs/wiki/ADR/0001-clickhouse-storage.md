# ADR 0001: ClickHouse Storage Layout

## Status

Accepted

## Context

The system needs to store a high-volume quote stream and serve range reads for line charts and candlestick charts. Typical reads filter by one symbol and a time range.

## Decision

Use `MergeTree` for `quotes_raw` with:

- `PARTITION BY toYYYYMM(event_time)`;
- `ORDER BY (symbol, event_time)`;
- `DateTime64(3, 'UTC')` timestamps;
- 180-day TTL for local hot historical data.

Use `AggregatingMergeTree` plus a materialized view for one-minute OHLC data.

## Alternatives

- PostgreSQL time-series table: simpler for team familiarity, but weaker for high-volume analytical range scans.
- Plain `MergeTree` candle table: easier to query, but repeated materialized-view inserts can create duplicate candle rows that require re-aggregation anyway.
- TimescaleDB: strong time-series option, but not requested by the team SRS and adds another stack.

## Consequences

Ktor should query `quotes_ohlc_1m_read` for candles. The internal aggregate-state table remains an implementation detail.

