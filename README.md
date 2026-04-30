# TradingExchange Unified Docker Stack

Единый запуск всей системы через корневой Docker Compose.

## Что поднимается

По умолчанию (`docker compose up`) запускаются:

- `postgres` - основная БД Ktor
- `redis` - стримы/кэш
- `clickhouse` - исторические котировки
- `mock-driver` + `collector` - поток котировок в ClickHouse
- `ktor` - основной backend API (`/api/v1/*`, WebSocket)
- `otel-collector`, `prometheus`, `loki`, `tempo`, `promtail`, `grafana`
- `redis-exporter`, `postgres-exporter`

Опционально:

- `api-mock` через профиль `api-mock`
- `android-vue-apk` job через профиль `apk`

## Быстрый старт

```bash
cp .env.example .env
docker compose up --build -d
```

Проверка:

```bash
docker compose ps
curl http://localhost:8080/health/live
curl http://localhost:8080/health/ready
```

## Профили

Поднять API mock дополнительно:

```bash
docker compose --profile api-mock up --build -d
```

## Сборка Android APK в том же compose

APK собирается отдельной командой (не на каждый `up`):

```bash
docker compose --profile apk run --rm android-vue-apk
```

APK после сборки:

`android-vue/android/app/build/outputs/apk/debug/app-debug.apk`

## Основные URL

- Ktor API: `http://localhost:8080`
- API Mock (profile): `http://localhost:8081`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Loki: `http://localhost:3100`
- Tempo: `http://localhost:3200`
- ClickHouse HTTP: `http://localhost:8123`

## Сквозная проверка (минимум)

1. Register: `POST /api/v1/auth/register`
2. Login: `POST /api/v1/auth/login`
3. Portfolio: `GET /api/v1/portfolio`
4. Buy/Sell: `POST /api/v1/orders`
5. Orders history: `GET /api/v1/orders`
6. Transactions history: `GET /api/v1/transactions`
7. WS handshake: `GET /api/v1/quotes/ws`

## Остановка

```bash
docker compose down
```

С очисткой томов:

```bash
docker compose down -v
```
