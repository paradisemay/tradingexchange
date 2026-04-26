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
