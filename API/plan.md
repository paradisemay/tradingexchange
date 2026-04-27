# API Module Plan

## Scope

The API module is a spec, docs, mock and contract-test layer. It does not run production business logic and does not proxy Ktor.

## Current Iteration

- [x] OpenAPI 3.1 REST contract for the current Ktor-compatible API.
- [x] AsyncAPI WebSocket contract for quote streaming.
- [x] JSON examples for happy-path and error-path payloads.
- [x] Dockerized Node mock server with REST, WebSocket, docs and smoke checks.
- [x] Obsidian wiki under `docs/wiki/modules/api/`.
- [x] ADR section for architectural decisions.
- [x] Environment-variable based mock tokens and ports, mirrored in `.env.example`.
- [x] Basic validation and smoke scripts.

## Next Small Tasks

- [ ] Add generated Postman or Insomnia collection from `docs/api/openapi.yaml`.
- [ ] Add contract tests against the real Ktor backend.
- [ ] Add CI job: validate specs, run smoke against mock container, publish docs artifacts.
- [ ] Add security scanning in CI (`npm audit` when lockfile/dependencies are introduced, container image scan).
- [ ] Keep every public API change paired with OpenAPI/AsyncAPI, examples, changelog and ADR updates when decisions change.

## Commit Policy

Use small logically complete commits in Conventional Commits format:

- `feat(api): add endpoint contract`
- `fix(api): align mock response with ktor`
- `docs(api): record websocket reconnect ADR`
- `test(api): add smoke scenario`
