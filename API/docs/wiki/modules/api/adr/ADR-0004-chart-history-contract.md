# ADR-0004: Chart History Contract

## Status

Accepted

## Context

`Мобилки.txt` defines ClickHouse as the historical quote store for raw points and OHLC candles. Mobile clients must not query ClickHouse directly; Ktor is the only backend consumer of ClickHouse.

## Decision

Expose chart data through REST endpoints:

- `GET /api/v1/instruments/{ticker}/chart/line`
- `GET /api/v1/instruments/{ticker}/chart/candles`

Both endpoints accept `range` and `interval`. `range` controls the visible period; `interval` controls point/candle granularity. `interval` must be strictly smaller than `range`. `1MIN` means one minute; `1M` means one month. Money values are decimal strings. Time is `timestampMs`.

REST chart responses are snapshots. Open clients should use WebSocket `quote` messages to update the currently visible chart after the snapshot is loaded.

The API mock stores quote ticks in memory as they are generated for WebSocket subscribers and builds chart responses from that accumulated history. It intentionally returns an empty chart when no accumulated ticks exist, rather than fabricating synthetic historical data.

## Alternatives

- Stream all chart history over WebSocket. Rejected because history is request/response data and can be cached separately from live quotes.
- Poll REST chart endpoints for every live tick. Rejected for the Android MVP because WebSocket quotes already provide lower-latency incremental updates.
- Let Android query ClickHouse HTTP directly. Rejected because it violates module boundaries and leaks analytics DB details to clients.

## Consequences

- OpenAPI is the source of truth for chart data.
- Ktor must translate these endpoints to ClickHouse queries.
- Android can render both regular and candlestick charts from the same API module.
