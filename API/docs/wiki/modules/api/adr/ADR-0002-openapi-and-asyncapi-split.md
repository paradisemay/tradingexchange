# ADR-0002: OpenAPI And AsyncAPI Split

## Status

Accepted

## Context

REST endpoints and WebSocket quotes have different interaction models. REST is request/response with HTTP status codes. Quotes are session-based streaming messages.

## Decision

Use OpenAPI 3.1 for REST and AsyncAPI 2.6 for WebSocket.

## Alternatives

- Put WebSocket notes only into OpenAPI descriptions. Rejected because message direction, reconnect behavior and subscription commands are clearer in AsyncAPI.
- Keep plain Markdown only. Rejected because generated docs, mocks and contract checks need machine-readable specs.

## Consequences

- `docs/api/openapi.yaml` is normative for REST.
- `docs/api/asyncapi.yaml` is normative for quotes WebSocket.
- Swagger UI is generated from OpenAPI; AsyncAPI is served as raw spec for now.
