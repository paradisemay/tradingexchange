# API Module Runbook

## Docker launch

```bash
cd API
cp .env.example .env
docker compose up --build
```

The service listens on `http://localhost:8081` by default.

## Environment variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `API_MOCK_PORT` | `8081` | Host port for the API mock/docs service |
| `QUOTE_INTERVAL_MS` | `1000` | WebSocket quote event interval |
| `MOCK_JWT` | `mock-access-token` | Token accepted by protected mock REST endpoints |
| `MOCK_REFRESH_TOKEN` | `mock-refresh-token` | Refresh token returned by register/login |
| `MOCK_REFRESH_TOKEN_ROTATED` | `mock-refresh-token-rotated` | Refresh token returned by refresh |

## Validate specs and examples

```bash
cd API
npm run validate
```

Validation checks:

- required spec and wiki files exist;
- OpenAPI and AsyncAPI contain required sections;
- current Ktor compatibility markers are present;
- JSON examples parse correctly.

## REST smoke checks

Start the server first, then run:

```bash
cd API
npm run smoke
```

The smoke script verifies:

- `/health`, `/health/live`, `/health/ready`;
- register, login, refresh, logout;
- profile, portfolio, instruments;
- line chart and candlestick chart history;
- create order, list orders, list transactions;
- WebSocket subscribe and quote receive.

## Manual REST examples

```bash
curl http://localhost:8081/health

curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"trader@example.com","password":"secret123"}'

curl http://localhost:8081/api/v1/portfolio \
  -H "Authorization: Bearer mock-access-token"
```

## Manual WebSocket example

Connect to:

```text
ws://localhost:8081/api/v1/quotes/ws?accessToken=mock-access-token
```

Send:

```json
{"type":"subscribe","tickers":["SBER"]}
```

Expected server event:

```json
{"type":"quote","ticker":"SBER","price":"252.0000","currency":"RUB","timestampMs":1775901000000}
```

## Common issues

- Port `8081` is busy: change `API_MOCK_PORT` in `.env`.
- Protected REST endpoint returns `401`: pass `Authorization: Bearer mock-access-token`.
- Swagger UI is blank: the UI HTML loads Swagger assets from CDN, so browser network access may be needed. Raw specs are still available at `/docs/openapi.yaml` and `/docs/asyncapi.yaml`.
- WebSocket connects but no events arrive: send a `subscribe` message first; subscriptions are not automatic.

## Security Checks

Before sharing a branch:

```powershell
npm run validate
npm run smoke
docker compose config
```

When npm dependencies are added, also run `npm audit --audit-level=moderate`.
