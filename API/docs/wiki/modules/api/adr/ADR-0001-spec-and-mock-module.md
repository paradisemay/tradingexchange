# ADR-0001: Spec And Mock Module

## Status

Accepted

## Context

The project has Ktor backend and two mobile clients. Integration breaks easily if DTO names, error formats or streaming messages drift between teams.

## Decision

Keep `API/` as an autonomous spec+mock module. It owns OpenAPI, AsyncAPI, examples, wiki and a Dockerized mock server.

## Alternatives

- Document contracts only inside Ktor code. Rejected because mobile teams need a backend-independent source of truth.
- Generate contracts from mobile DTOs. Rejected because the backend remains the authority for behavior and status codes.

## Consequences

- Any public contract change must update specs, examples and wiki.
- Backend implementation must be checked against this module.
- The mock is not production gateway logic.
