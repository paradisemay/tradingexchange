# API Module

## Назначение

API-модуль является единым контрактным слоем между Ktor backend и мобильными клиентами Android Kotlin / Android Vue. Он не содержит бизнес-логики брокера и не ходит в PostgreSQL, Redis или ClickHouse. Его задача - зафиксировать публичные форматы обмена и дать mock-сервер для ранней интеграции клиентов.

В этой итерации спецификация совместима с текущей реализацией Ktor:

- `GET /api/v1/instruments` возвращает массив, а не объект `{items: [...]}`.
- `GET /api/v1/orders` возвращает `{orders, nextCursor}`.
- `GET /api/v1/transactions` возвращает `{transactions, nextCursor}`.
- `cursor` в списках - ISO-8601 `createdAt`, а не opaque cursor.
- `INSUFFICIENT_FUNDS` и `INSUFFICIENT_POSITION` возвращаются как HTTP `422`.
- `QUOTE_UNAVAILABLE` возвращается как HTTP `503`.

## Состав

```text
API/
  docs/api/openapi.yaml       REST contract, OpenAPI 3.1
  docs/api/asyncapi.yaml      WebSocket contract, AsyncAPI
  docs/api/examples/          JSON payload examples
  docs/wiki/modules/api/      module wiki
  server/index.js             REST + WebSocket mock runtime
  scripts/validate.js         local contract sanity checks
  scripts/smoke.js            REST + WebSocket smoke scenario
  Dockerfile
  docker-compose.yml
```

## Ответственность модуля

- Хранит нормативные REST и WebSocket спецификации.
- Публикует примеры payload для мобильных клиентов и тестов.
- Поднимает mock-сервер без Ktor, БД и Redis.
- Документирует текущие отличия backend от идеального Mini-SRS.

## Что модуль не делает

- Не проксирует запросы в Ktor.
- Не исполняет реальные сделки.
- Не гарантирует persistence между перезапусками.
- Не заменяет интеграционные тесты настоящего backend.

## Основные URL при запуске

- `http://localhost:8081/docs` - индекс документации.
- `http://localhost:8081/docs/swagger` - Swagger UI.
- `http://localhost:8081/docs/openapi.yaml` - raw OpenAPI.
- `http://localhost:8081/docs/asyncapi.yaml` - raw AsyncAPI.
- `ws://localhost:8081/api/v1/quotes/ws?accessToken=mock-access-token` - WebSocket mock.

## ADR And Planning

- `API/plan.md` records the current decomposition, next tasks and Conventional Commits policy.
- `docs/wiki/modules/api/adr/` stores architecture decision records.
- `docs/wiki/modules/api/security.md` stores security rules and early checks.

Update these files together with code and specs when a decision changes.
