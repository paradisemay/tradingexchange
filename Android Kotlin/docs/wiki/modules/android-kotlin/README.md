# Android Kotlin Module

## Role

The Android Kotlin module is the native mobile trading terminal. It consumes only the public API contract exposed by Ktor/API mock and never talks directly to PostgreSQL, Redis, ClickHouse, Go, or the C driver.

## Implemented MVP

- Login/register/logout.
- Session restore through DataStore.
- Portfolio with cash, positions and live quote updates.
- Instrument search.
- BUY/SELL order creation.
- Orders history.
- Transactions history.
- Profile screen.
- WebSocket quote subscription with reconnect.
- Room cache for screen data.

## Project Shape

The project uses one Gradle `app` module for educational maintainability. Clean Architecture boundaries are package-level:

```text
core/di          Hilt graph and app-wide providers
data/remote     Retrofit DTOs, API interface, error mapping
data/websocket  OkHttp WebSocket quotes manager
data/local      DataStore token storage, Room entities and DAO
data/repository repository implementations
domain/model    domain models, enums, mappers
domain/usecase  app use cases
ui/*            Compose screens and ViewModels
```

## Backend Defaults

Debug builds use API mock:

```text
http://10.0.2.2:8081/
```

The emulator uses `10.0.2.2` to reach the developer machine. Use `localhost` only from the host OS, not inside the emulator.
