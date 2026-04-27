# ADR-0003: Environment Configuration

## Status

Accepted

## Context

Ports and tokens must not be buried in implementation without a visible configuration surface.

## Decision

Mock runtime configuration is read from environment variables and documented in `.env.example`.

Current variables:

- `API_MOCK_PORT`
- `QUOTE_INTERVAL_MS`
- `MOCK_JWT`
- `MOCK_REFRESH_TOKEN`
- `MOCK_REFRESH_TOKEN_ROTATED`

## Alternatives

- Keep constants in `server/index.js`. Rejected because tests, Docker and local runs need predictable overrides.
- Require a real secret manager. Rejected because this module is a local mock, not a production service.

## Consequences

- Every new variable must be added to `.env.example`.
- Mock defaults are safe test values only and must not be reused as production credentials.
