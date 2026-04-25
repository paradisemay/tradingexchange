# Ktor Broker Backend

Бэкенд брокерского приложения: REST API + WebSocket котировок.  
Стек: **Kotlin / Ktor · PostgreSQL · Redis · HikariCP · Flyway · Argon2id · JWT · OpenTelemetry**.

---

## Требования

| Инструмент | Версия |
|---|---|
| Docker + Docker Compose | 24+ |
| JDK | 21 (для локальной разработки) |
| Gradle | поставляется через `./gradlew` |

---

## Быстрый старт (Docker Compose)

```bash
# 1. Скопировать конфиг окружения
cp .env.example .env
# Отредактировать .env: выставить JWT_SECRET и POSTGRES_PASSWORD

# 2. Поднять всё одной командой
docker compose up --build

# Сервис будет доступен на http://localhost:8080
```

Стек поднимается в таком порядке (через healthcheck-зависимости):
1. **postgres** — ждёт `pg_isready`
2. **redis** — ждёт `PING`
3. **ktor** — запускает Flyway-миграции, затем HTTP

Проверить готовность:
```bash
curl http://localhost:8080/health/ready
# {"status":"UP","db":"UP","redis":"UP"}
```

---

## Локальная разработка (без Docker)

### 1. Запустить инфраструктуру

```bash
# Только postgres + redis
docker compose up postgres redis -d
```

### 2. Настроить переменные окружения

```bash
cp .env.example .env
# Установить: POSTGRES_HOST=localhost, REDIS_HOST=localhost
# и остальные переменные из .env.example
```

### 3. Запустить сервер

```bash
# Загрузить .env и запустить через Gradle
export $(grep -v '^#' .env | xargs)
./gradlew :server:run
```

Или запустить напрямую из IDE (IntelliJ IDEA): запускаем `com.example.main` с переменными из `.env`.

---

## Переменные окружения

| Переменная | По умолчанию | Описание |
|---|---|---|
| `POSTGRES_HOST` | `localhost` | Хост PostgreSQL |
| `POSTGRES_PORT` | `5432` | Порт PostgreSQL |
| `POSTGRES_DB` | `broker` | Название БД |
| `POSTGRES_USER` | `broker_app` | Пользователь БД |
| `POSTGRES_PASSWORD` | — | **Обязательно задать** |
| `POSTGRES_MAX_POOL_SIZE` | `30` | Макс. размер пула HikariCP |
| `POSTGRES_CONNECTION_TIMEOUT_MS` | `3000` | Таймаут подключения |
| `JWT_SECRET` | — | **Обязательно задать** (мин. 32 символа) |
| `JWT_ACCESS_TTL_MINUTES` | `15` | Время жизни access-токена |
| `JWT_REFRESH_TTL_DAYS` | `30` | Время жизни refresh-токена |
| `REDIS_HOST` | `localhost` | Хост Redis |
| `REDIS_PORT` | `6379` | Порт Redis |
| `REDIS_PASSWORD` | — | Пароль Redis (если задан) |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://otel-collector:4318` | Эндпоинт OpenTelemetry Collector |
| `PORT` | `8080` | HTTP-порт сервера |

Полный пример — в файле [`.env.example`](.env.example).

---

## API

Полная спецификация — [`swagger.yaml`](swagger.yaml) (OpenAPI 3.0).

### Auth
```
POST /api/v1/auth/register   — регистрация
POST /api/v1/auth/login      — вход
POST /api/v1/auth/refresh    — ротация refresh-токена
POST /api/v1/auth/logout     — выход
```

### Брокерский API (требует Bearer JWT)
```
GET  /api/v1/me                         — профиль пользователя
GET  /api/v1/portfolio                  — позиции + баланс
GET  /api/v1/instruments?query=SBER     — поиск инструментов
POST /api/v1/orders                     — создать заявку BUY/SELL
GET  /api/v1/orders?limit=50&cursor=    — история заявок
GET  /api/v1/transactions?limit=50&cursor= — история транзакций
WS   /api/v1/quotes/ws                  — котировки в реальном времени
```

### Health
```
GET /health/live   — liveness probe
GET /health/ready  — readiness probe (db + redis)
```

### Пример: регистрация и покупка

```bash
# Зарегистрироваться
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"trader@example.com","password":"secret123","fullName":"Ivan"}' \
  | jq -r .accessToken)

# Купить 10 акций SBER по рынку
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"ticker":"SBER","side":"BUY","orderType":"MARKET","quantity":"10"}'
```

### WebSocket котировок

```bash
# Подключиться (wscat: npm i -g wscat)
wscat -c "ws://localhost:8080/api/v1/quotes/ws" \
      -H "Authorization: Bearer $TOKEN"

# Подписаться на тикеры
> {"type":"subscribe","tickers":["SBER","GAZP"]}

# Сервер отвечает при каждом тике:
< {"type":"quote","ticker":"SBER","price":"252.0000","currency":"RUB","timestampMs":1775901000000}

# Отписаться
> {"type":"unsubscribe","tickers":["SBER"]}
```

---

## Сборка и тесты

```bash
# Собрать без тестов
./gradlew :server:build -x test

# Запустить тесты (требуют запущенных postgres + redis или Testcontainers)
./gradlew :server:test

# Собрать Docker-образ вручную
docker build -t broker-ktor .
```

---

## Структура проекта

```
ktor/
├── docker-compose.yml       — инфраструктура (postgres, redis, exporters, ktor)
├── Dockerfile
├── .env.example             — шаблон переменных окружения
├── swagger.yaml             — OpenAPI 3.0 спецификация
├── plan.md                  — план работ и статусы задач
└── server/src/main/
    ├── kotlin/com/example/
    │   ├── Application.kt   — точка входа Ktor-модуля
    │   ├── main.kt
    │   ├── plugins/         — Security, Monitoring, Serialization, StatusPages, Websockets, OpenTelemetry
    │   ├── config/          — AppConfig (все ENV-переменные)
    │   ├── db/              — DatabaseFactory (HikariCP + Flyway), Transaction
    │   ├── domain/          — Models, Errors (AppException, ErrorCode)
    │   ├── auth/            — JWT, Argon2id, register/login/refresh/logout
    │   ├── instruments/     — справочник инструментов
    │   ├── portfolio/       — баланс и позиции
    │   ├── orders/          — BUY/SELL с SELECT FOR UPDATE
    │   ├── transactions/    — история финансовых операций
    │   ├── quotes/          — Redis Pub/Sub, PriceCache, WebSocket
    │   └── health/          — /health/live, /health/ready
    └── resources/
        ├── application.yaml
        ├── logback.xml      — JSON-логи (logstash)
        └── db/migration/    — V001 schema, V002 indexes, V003 seed
```

---

## Мониторинг

При поднятом стеке телеметрии метрики доступны через экспортеры:

| Сервис | Порт | Метрики |
|---|---|---|
| redis-exporter | 9121 | `redis_up`, `redis_memory_used_bytes`, `redis_connected_clients`, ... |
| postgres-exporter | 9187 | `pg_up`, `pg_stat_database_numbackends`, `pg_locks_count`, ... |
| ktor (OTLP) | → otel-collector:4318 | HTTP latency, 4xx/5xx, WebSocket сессии, DB pool |

---

## Безопасность

- Пароли хэшируются **Argon2id** (3 итерации, 64 МБ памяти)
- Все секреты — только через переменные окружения, хардкод запрещён
- Refresh-токены хранятся в БД **хэшом SHA-256**, ротируются при каждом использовании
- SQL — только через **prepared statements**, ORM не используется
- Снятые уязвимости: CVE-2024-25710, CVE-2025-48924 — см. [`docs/implementation-report.md`](docs/implementation-report.md)
