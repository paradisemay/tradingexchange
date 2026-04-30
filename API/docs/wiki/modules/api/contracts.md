# API Contracts

## Общие правила JSON

- Публичные поля используют `camelCase`.
- REST request/response передаются как `application/json; charset=utf-8`.
- Успешный ответ не оборачивается в общий envelope `{data: ...}`.
- Денежные значения и количество бумаг передаются строками: `"252.0000"`, `"10"`.
- Даты REST передаются как ISO-8601 UTC string.
- В WebSocket время котировки передается как `timestampMs`.
- `null` означает отсутствие значения, а не пустую строку или ноль.

## Авторизация

REST защищенные endpoints требуют:

```http
Authorization: Bearer <accessToken>
```

WebSocket `/api/v1/quotes/ws` принимает два варианта:

- `Authorization: Bearer <accessToken>`;
- fallback `?accessToken=<token>`.

Fallback нужен для мобильных клиентов, где WebSocket handshake может не позволять удобно задать custom header.

## REST endpoints

Текущий набор публичных endpoints:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/me`
- `GET /api/v1/portfolio`
- `GET /api/v1/instruments?query=SBER`
- `GET /api/v1/instruments/{ticker}/chart/line?range=1D&interval=5m`
- `GET /api/v1/instruments/{ticker}/chart/candles?range=1D&interval=5m`
- `POST /api/v1/orders`
- `GET /api/v1/orders?limit=50&cursor=...`
- `GET /api/v1/transactions?limit=50&cursor=...`
- `GET /health/live`
- `GET /health/ready`

## Response shapes from current Ktor

`POST /api/v1/auth/register`:

```json
{
  "userId": "8e2f8d1a-1d50-4fb3-b3ea-2f88d7cbb2b1",
  "accessToken": "jwt",
  "refreshToken": "token"
}
```

`POST /api/v1/auth/login` and `POST /api/v1/auth/refresh`:

```json
{
  "accessToken": "jwt",
  "refreshToken": "token"
}
```

`GET /api/v1/instruments`:

```json
[
  {
    "ticker": "SBER",
    "name": "Sberbank",
    "currency": "RUB",
    "lotSize": 1,
    "isActive": true,
    "lastPrice": "252.0000"
  }
]
```

`GET /api/v1/orders`:

```json
{
  "orders": [],
  "nextCursor": null
}
```

`GET /api/v1/transactions`:

```json
{
  "transactions": [],
  "nextCursor": null
}
```

## Chart contracts

Historical chart data belongs to ClickHouse, but mobile clients access it only through Ktor/API.

Line chart response:

```json
{
  "ticker": "SBER",
  "currency": "RUB",
  "range": "1MIN",
  "interval": "1s",
  "points": [
    { "timestampMs": 1775901000000, "price": "252.0000" }
  ]
}
```

Candlestick response:

```json
{
  "ticker": "SBER",
  "currency": "RUB",
  "range": "1H",
  "interval": "1m",
  "candles": [
    {
      "timestampMs": 1775901000000,
      "open": "251.5000",
      "high": "253.0000",
      "low": "251.0000",
      "close": "252.0000"
    }
  ]
}
```

Supported ranges: `1MIN`, `1H`, `1D`, `1W`, `1M`, `6M`, `1Y`.
Supported intervals: `1s`, `1m`, `5m`, `15m`, `1h`, `1d`.

`range` controls the visible period. `interval` controls point/candle granularity inside that period. `1MIN` means one minute; `1M` means one month. `interval` must be strictly smaller than `range`; invalid combinations return `400 VALIDATION_ERROR`.

For long ranges Ktor should prefer pre-aggregated OHLC data from ClickHouse. For short line charts Ktor may use raw points or a downsampled series.

API mock chart history is built only from quote ticks accumulated since mock-server startup. If no ticks exist for the requested ticker and range, the response contains an empty `points` or `candles` array.

Chart responses are capped by the selected window: `floor(range / interval)` buckets, with a defensive maximum of 160 buckets for long ranges.

Open chart screens use REST as the initial snapshot and WebSocket `quote` messages as live incremental updates. Line charts update the current interval bucket instead of appending every tick.

## Error contract

Application errors use:

```json
{
  "errorCode": "INSUFFICIENT_FUNDS",
  "message": "Insufficient funds",
  "details": {
    "required": "2520.0000",
    "available": "1000.0000"
  },
  "traceId": ""
}
```

Current Ktor JWT challenge is a compatibility exception:

```json
{
  "errorCode": "UNAUTHORIZED",
  "message": "Token is missing or invalid"
}
```

## Compatibility notes against Mini-SRS

The course Mini-SRS describes the target API shape. The current backend has a few differences that are intentionally encoded in this module so clients can integrate now:

- `items` is not used for instruments, orders, or transactions.
- Cursor pagination is based on `createdAt` only.
- `GET /api/v1/me` does not return `createdAt`.
- Transaction item uses `id`, not `transactionId`, and does not expose `orderId`.
- Order response does not expose `price`.
- `traceId` may be an empty string.

Future alignment should be done through a backend change and a versioned contract update.
