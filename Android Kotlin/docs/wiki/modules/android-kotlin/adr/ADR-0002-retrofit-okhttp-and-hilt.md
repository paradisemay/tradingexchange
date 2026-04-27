# ADR-0002: Retrofit, OkHttp And Hilt

## Status

Accepted

## Context

The app needs REST, WebSocket, token refresh, timeout handling, dependency injection and testable repositories.

## Decision

Use:

- Retrofit for REST.
- OkHttp for HTTP transport, interceptors, authenticator and WebSocket.
- kotlinx.serialization for JSON.
- Hilt for dependency injection.

## Alternatives

- Ktor Client. Rejected because Retrofit + OkHttp has simpler Android integration for interceptors and WebSocket in this MVP.
- Manual DI. Rejected because lifecycle-aware ViewModel injection would become repetitive and fragile.

## Consequences

- Access token is injected through an OkHttp interceptor.
- Refresh is synchronized by `Mutex` in `TokenAuthenticator`.
- HTTP calls have explicit timeouts to isolate slow or unavailable containers.
