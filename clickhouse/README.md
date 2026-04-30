# ClickHouse Module

ClickHouse stores historical quote time series for charts and analytical reads. Mobile clients never connect to ClickHouse directly; Ktor reads prepared data and exposes an application API.

## Unified stack note

Для end-to-end запуска всей платформы используйте корневой `docker-compose.yml`.
Локальный compose в этом модуле нужен для изолированной отладки ClickHouse.

## Scope

This module owns:

- local ClickHouse Docker Compose setup;
- quote storage schema;
- materialized one-minute OHLC aggregation;
- mock data and smoke query scripts;
- Ktor-facing read contract documentation.

It does not own Ktor runtime code, Redis Streams, order processing, or user/session storage.

## Configuration

Copy the example file and replace the placeholder password:

```powershell
cd C:\Users\yarik\GolandProjects\tradingexchange\clickhouse
Copy-Item .env.example .env
notepad .env
```

Important variables:

| Variable | Purpose |
| --- | --- |
| `CLICKHOUSE_HTTP_PORT` | Host HTTP port, bound to `127.0.0.1` |
| `CLICKHOUSE_TCP_PORT` | Host native TCP port for Go, bound to `127.0.0.1` |
| `CLICKHOUSE_DATABASE` | Database created on startup |
| `CLICKHOUSE_USER` | Application user |
| `CLICKHOUSE_PASSWORD` | Local password, required |

Do not commit real `.env` files or real passwords.

## Start

```powershell
docker compose up -d
docker compose ps
```

The init script creates:

- `trading.quotes_raw` for raw quote ticks;
- `trading.quotes_ohlc_1m` for aggregate states;
- `trading.quotes_ohlc_1m_mv` materialized view;
- `trading.quotes_ohlc_1m_read` read view for Ktor.

## Mock Data

```powershell
.\scripts\load-mock-data.ps1
```

This inserts 100 000 sample quotes for `SBER`, `GAZP`, `YNDX`, `LKOH`, and `ROSN`.

## Smoke Queries

```powershell
.\scripts\smoke-test.ps1 -Symbol SBER
```

The script verifies raw points, one-minute candles, and summary aggregation.

## Go Producer Contract

Go writes through native TCP on port `9000` using batch insert into `trading.quotes_raw`.

Columns:

| Column | Type | Source |
| --- | --- | --- |
| `symbol` | `LowCardinality(String)` | `model.Quote.Ticker` |
| `event_time` | `DateTime64(3, 'UTC')` | `model.Quote.TimestampMs` |
| `price` | `Float64` | `model.Quote.Price` |
| `ingested_at` | `DateTime64(3, 'UTC')` | insert time |

Rules:

- batch size is controlled by `CLICKHOUSE_BATCH_SIZE`, default `10000`;
- timed flush is controlled by `CLICKHOUSE_FLUSH_INTERVAL_MS`, default `200`;
- single-row production inserts are not used;
- retry with backoff is required for temporary errors;
- graceful shutdown flushes the remaining buffer.

## Ktor Read Contract

Ktor should read:

- raw line chart points from `trading.quotes_raw`;
- candlestick data from `trading.quotes_ohlc_1m_read`;
- range summary from `trading.quotes_raw`.

See [openapi/ktor-quotes-history.yaml](openapi/ktor-quotes-history.yaml) and [sql/020_sample_queries.sql](sql/020_sample_queries.sql).

