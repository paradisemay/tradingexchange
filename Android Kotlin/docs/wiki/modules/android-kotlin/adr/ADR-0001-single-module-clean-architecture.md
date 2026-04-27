# ADR-0001: Single Module Clean Architecture

## Status

Accepted

## Context

The educational project needs a complete Android MVP without Gradle module overhead slowing down setup.

## Decision

Use one Gradle `app` module with package-level Clean Architecture:

- `data`
- `domain`
- `ui`
- `core`

Presentation uses MVVM with unidirectional data flow.

## Alternatives

- Multi-module Gradle project. Rejected for this iteration because it increases setup cost before the team has stable feature boundaries.
- Activity-centric architecture. Rejected because it makes network, cache and state testing harder.

## Consequences

- Package boundaries must be respected manually.
- DTOs stay in `data`; Compose screens do not consume DTOs directly.
- Future split into Gradle modules remains possible.
