# API Integration

## REST

The app follows the current `API/docs/api/openapi.yaml` contract.

Important current-Ktor shapes:

- `POST /api/v1/auth/login` returns `{accessToken, refreshToken}` without `userId`.
- `GET /api/v1/instruments` returns an array, not `{items}`.
- Instrument price field is `lastPrice`.
- `GET /api/v1/orders` returns `{orders, nextCursor}`.
- `GET /api/v1/transactions` returns `{transactions, nextCursor}`.
- Transaction id field is `id`.

## Auth

`AuthInterceptor` adds:

```http
Authorization: Bearer <accessToken>
```

`TokenAuthenticator` handles `401`:

1. Reads refresh token from DataStore.
2. Calls `POST /api/v1/auth/refresh`.
3. Saves the new token pair.
4. Retries the original request once.
5. Clears tokens if refresh fails.

The refresh path is protected by a `Mutex` to avoid multiple parallel refresh calls.

## WebSocket

Endpoint in debug:

```text
ws://127.0.0.1:8081/api/v1/quotes/ws
```

The app uses query-token fallback:

```text
?accessToken=<token>
```

`API_BASE_URL` and `WS_BASE_URL` are build-time configuration values. They can be provided through Gradle properties or environment variables and must not be changed by editing source for each developer machine.

Client commands:

```json
{"type":"subscribe","tickers":["SBER"]}
{"type":"unsubscribe","tickers":["SBER"]}
```

Server event:

```json
{"type":"quote","ticker":"SBER","price":"252.0000","currency":"RUB","timestampMs":1775901000000}
```

The instruments screen uses REST search results as the visible list source of truth. Room remains a cache, but it does not expand the current search result with stale instruments. The screen subscribes to WebSocket quotes only for currently visible tickers and renders `quote.price` over `lastPrice`.

## Historical Charts

Android renders chart UI, but does not calculate historical market data and does not talk to ClickHouse.

REST endpoints:

```text
GET /api/v1/instruments/{ticker}/chart/line?range=1MIN&interval=1s
GET /api/v1/instruments/{ticker}/chart/candles?range=1H&interval=1m
```

Line point:

```json
{ "timestampMs": 1775901000000, "price": "252.0000" }
```

Candle:

```json
{ "timestampMs": 1775901000000, "open": "251.5000", "high": "253.0000", "low": "251.0000", "close": "252.0000" }
```

The app keeps all money values as `String` at DTO level and maps them to `BigDecimal` in domain mappers.

`range` controls the visible period. Supported ranges are `1MIN`, `1H`, `1D`, `1W`, `1M`, `6M`, `1Y`; the UI shows `1MIN` as `1m` and keeps `1M` as one month. `interval` controls point/candle granularity. Supported intervals are `1s`, `1m`, `5m`, `15m`, `1h`, `1d`. The UI hides intervals that are greater than or equal to the selected range.

Chart screens load a REST snapshot first, then subscribe to WebSocket quotes for the open ticker. Line charts update the current interval bucket or create a new point only when a new bucket begins; candlestick charts update the current interval bucket or create a new candle.

Live chart state is trimmed by the selected range window. The app keeps at most `floor(range / interval)` buckets, with a defensive cap of 160 buckets for long ranges.

Reconnect policy:

```text
1s -> 2s -> 5s -> 10s -> 30s
```

After reconnect, all active subscriptions are sent again.

## Error Mapping

The app maps `errorCode`, not human text:

- `UNAUTHORIZED` -> relogin / refresh flow.
- `VALIDATION_ERROR` -> form error.
- `INSUFFICIENT_FUNDS` -> buy operation error.
- `INSUFFICIENT_POSITION` -> sell operation error.
- `QUOTE_UNAVAILABLE` -> temporary market price issue.
- missing `details/traceId` in auth challenge is accepted for current Ktor compatibility.
