# API Spec+Mock Module

This module is the contract source for mobile clients and backend integration.
It does not proxy to Ktor and does not require PostgreSQL, Redis, Go, or the C driver.

## Quick Start

```bash
cd API
cp .env.example .env
docker compose up --build
```

Open:

- Docs: http://localhost:8081/docs
- Swagger UI: http://localhost:8081/docs/swagger
- OpenAPI: http://localhost:8081/docs/openapi.yaml
- AsyncAPI: http://localhost:8081/docs/asyncapi.yaml
- Mock health: http://localhost:8081/health

## Local Checks

```bash
npm run validate
npm run smoke
```

`npm run smoke` expects the mock server to be running at `http://localhost:8081`.

## Configuration

Runtime settings come from environment variables. When a new variable is added, update `.env.example`.

```text
API_MOCK_PORT=8081
QUOTE_INTERVAL_MS=1000
MOCK_JWT=mock-access-token
MOCK_REFRESH_TOKEN=mock-refresh-token
MOCK_REFRESH_TOKEN_ROTATED=mock-refresh-token-rotated
```

## Planning And Wiki

- `plan.md` tracks module tasks and commit policy.
- `docs/wiki/modules/api/` contains Obsidian-ready wiki pages.
- `docs/wiki/modules/api/adr/` contains ADRs for architecture decisions.
- `docs/wiki/modules/api/security.md` contains local security rules and checks.
