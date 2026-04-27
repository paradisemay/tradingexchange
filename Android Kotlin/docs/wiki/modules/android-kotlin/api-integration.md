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
