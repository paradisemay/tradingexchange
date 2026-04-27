# Mock Server

## Role

The mock server gives mobile developers a stable integration target before or without a running Ktor backend. It mirrors the current public Ktor response shapes but returns deterministic in-memory data.

## Runtime

The implementation is `server/index.js` and uses only the Node.js standard library:

- `http` for REST and documentation routes;
- manual WebSocket handshake and text frame parsing;
- in-memory arrays for instruments, orders and transactions;
- timer-based quote generation for subscribed tickers.

No npm dependencies are required at runtime.

## REST behavior

The mock supports:

- auth endpoints with environment-configured mock tokens;
- protected REST endpoints that accept `Authorization: Bearer <MOCK_JWT>`;
- Ktor-compatible response fields;
- basic validation errors;
- in-memory order and transaction insertion after `POST /api/v1/orders`.

The server intentionally does not implement real account math or persistence. It is a contract mock, not a business simulator.

## WebSocket behavior

Endpoint:

```text
/api/v1/quotes/ws
```

Accepted auth:

- `Authorization: Bearer <MOCK_JWT>`;
- `?accessToken=<MOCK_JWT>`.

Default local value is `mock-access-token`. Override it through `.env` or Docker environment.

Client messages:

```json
{"type":"subscribe","tickers":["SBER","GAZP"]}
{"type":"unsubscribe","tickers":["SBER"]}
```

Server sends a quote every `QUOTE_INTERVAL_MS` for every ticker currently subscribed by that socket session.

Subscription state is per connection. After reconnect, the client must send `subscribe` again, matching the Ktor contract.

## Documentation routes

- `/docs` - documentation index.
- `/docs/swagger` - Swagger UI for `openapi.yaml`.
- `/docs/openapi.yaml` - raw REST contract.
- `/docs/asyncapi.yaml` - raw WebSocket contract.
- `/docs/api/examples/*.json` - payload examples.

## Extending the mock

When Ktor changes a public field or endpoint:

1. Update `docs/api/openapi.yaml` or `docs/api/asyncapi.yaml`.
2. Update the relevant examples in `docs/api/examples/`.
3. Update `server/index.js` only if the mock behavior or response shape changed.
4. Update compatibility notes in `contracts.md`.
5. Run `npm run validate` and `npm run smoke`.
