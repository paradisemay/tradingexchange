# Architecture

## Layers

The app follows Clean Architecture with pragmatic package boundaries.

- `data`: remote REST, WebSocket, local cache and repository implementations.
- `domain`: models, repository interfaces, use cases and business-safe types.
- `ui`: Compose screens, ViewModels, UiState and user actions.

Dependencies point inward:

```text
ui -> domain <- data
```

The `domain` layer does not depend on Android, Retrofit, Room or Compose.

## Presentation Pattern

Screens use MVVM with unidirectional data flow:

```text
Compose action -> ViewModel -> StateFlow<UiState> -> Compose render
```

Rules:

- Compose does not call repositories directly.
- ViewModels expose immutable `StateFlow`.
- One-off messages are represented as state messages in this MVP.
- DTOs are never rendered directly by Compose.

## Navigation

The app has one `NavHost` and authenticated bottom navigation:

- `auth`
- `portfolio`
- `instruments`
- `orders`
- `transactions`
- `profile`

`SessionViewModel` observes token presence and redirects between `auth` and protected screens.

## DI

Hilt owns:

- Retrofit API clients.
- OkHttp clients, interceptor and authenticator.
- Room database and DAO.
- Repositories and use cases.
- WebSocket repository.
