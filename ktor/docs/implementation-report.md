# Отчёт о реализации: Ktor Broker Backend

**Дата:** 2026-04-25  
**Ветка:** `feature/ktor-broker-backend`  
**Автор:** weebat

---

## Что было сделано

### Модуль 1 — PostgreSQL (миграции и схема)

#### Flyway-миграции (`server/src/main/resources/db/migration/`)

| Файл | Содержание |
|---|---|
| `V001__init.sql` | Создание 7 таблиц: `users`, `accounts`, `instruments`, `portfolio_positions`, `orders`, `transactions`, `sessions`. Check-constraints (cash_balance ≥ 0, quantity ≥ 0, lot_size > 0), foreign keys, UUID primary keys через `uuid-ossp`, CITEXT для email. |
| `V002__indexes.sql` | 5 индексов: `idx_orders_user_created_at`, `idx_orders_status_created_at`, `idx_transactions_user_created_at`, `idx_positions_user`, `idx_sessions_user_active (WHERE revoked_at IS NULL)`. |
| `V003__seed.sql` | Seed-данные: инструменты SBER, GAZP, YNDX, LKOH, GMKN с начальными ценами. |

Миграции запускаются автоматически через Flyway при старте Ktor — до поднятия HTTP-роутинга.

---

### Модуль 2 — Ktor Backend

#### 2.1 Сборка и зависимости (`gradle/libs.versions.toml`, `server/build.gradle.kts`)

Добавленные библиотеки:
- **HikariCP 5.1.0** — connection pool (POSTGRES_MAX_POOL_SIZE=30, timeout 3 000 ms)
- **Flyway 10.21.0 + flyway-database-postgresql** — DB-миграции
- **Lettuce 6.5.5** — асинхронный Redis-клиент
- **Argon2-jvm 2.11** — хэширование паролей (Argon2id, 3 итерации, 64 МБ)
- **logstash-logback-encoder 8.1** — структурные JSON-логи

#### 2.2 Конфигурация (`config/AppConfig.kt`)

Все параметры — только из переменных окружения. Никакого хардкода. Пример `.env.example`:
```
POSTGRES_HOST, POSTGRES_PORT, POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD
JWT_SECRET, JWT_ACCESS_TTL_MINUTES=15, JWT_REFRESH_TTL_DAYS=30
REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
OTEL_EXPORTER_OTLP_ENDPOINT
```

#### 2.3 Слои приложения

```
routing (AuthRoutes, OrderRoutes, ...) 
  └─ service (AuthService, OrderService)
       └─ repository (UserRepository, OrderRepository, ...)
            ├─ PostgreSQL via HikariCP + plain JDBC (prepared statements)
            └─ Redis via Lettuce Pub/Sub
```

Блокирующие JDBC-вызовы выполняются на `Dispatchers.IO`. Транзакции через `DataSource.withTransaction {}`.

#### 2.4 Авторизация (`auth/`)

- **Регистрация:** Argon2id-хэш пароля → запись в `users` + `accounts` (начальный баланс 100 000 RUB) + `sessions`
- **Логин:** проверка пароля через `argon2.verify()` → выдача JWT access (15 мин) + refresh (30 дней)
- **Ротация refresh:** старый токен немедленно инвалидируется (`revoked_at`), выдаётся новый
- **Logout:** `revoked_at = NOW()` для сессии
- **JWT middleware:** `authenticate("auth-jwt")` на всех защищённых роутах, извлечение `userId` из claims

#### 2.5 REST API

| Метод | Путь | Описание |
|---|---|---|
| POST | `/api/v1/auth/register` | Регистрация → `{userId, accessToken, refreshToken}` |
| POST | `/api/v1/auth/login` | Логин → `{accessToken, refreshToken}` |
| POST | `/api/v1/auth/refresh` | Ротация refresh-токена |
| POST | `/api/v1/auth/logout` | Отзыв refresh-токена → 204 |
| GET | `/api/v1/me` | Профиль пользователя |
| GET | `/api/v1/portfolio` | Позиции + cash (currentPrice из Redis или null) |
| GET | `/api/v1/instruments?query=` | Поиск инструментов (ILIKE по ticker + name) |
| POST | `/api/v1/orders` | Создание заявки BUY/SELL MARKET/LIMIT |
| GET | `/api/v1/orders?limit=&cursor=` | История заявок (cursor-пагинация по created_at) |
| GET | `/api/v1/transactions?limit=&cursor=` | История транзакций |
| GET | `/health/live` | Liveness probe → `{"status":"UP"}` |
| GET | `/health/ready` | Readiness probe → `{"status":"UP","db":"UP","redis":"UP"}` |
| WS | `/api/v1/quotes/ws` | WebSocket котировок |

#### 2.6 Финансовая логика BUY/SELL (`orders/OrderService.kt`)

Критическая часть: все операции в **одной DB-транзакции**, порядок блокировок строго фиксирован.

**BUY:**
```
SELECT ... FROM accounts FOR UPDATE          ← блокировка 1
SELECT ... FROM portfolio_positions FOR UPDATE ← блокировка 2
проверка cash_balance >= total
INSERT INTO orders (status='FILLED')
UPDATE accounts SET cash_balance = cash_balance - total
INSERT/UPDATE portfolio_positions (avg_price пересчитывается)
INSERT INTO transactions (type='BUY')
COMMIT
```

**SELL:**
```
SELECT ... FROM accounts FOR UPDATE          ← блокировка 1
SELECT ... FROM portfolio_positions FOR UPDATE ← блокировка 2
проверка quantity >= requested
UPDATE portfolio_positions (quantity=0 → avg_price=0)
UPDATE accounts SET cash_balance = cash_balance + total
INSERT INTO orders (status='FILLED')
INSERT INTO transactions (type='SELL')
COMMIT
```

Порядок `accounts → portfolio_positions` обязателен для исключения deadlock при параллельных запросах одного пользователя.

Все денежные значения — `BigDecimal`. `Double`/`Float` запрещены.

#### 2.7 Redis + WebSocket (`quotes/`)

- **`RedisSubscriber`** подписывается на Pub/Sub канал `quotes.ticks`
- Reconnect с экспоненциальным backoff: 1s → 2s → 5s → 10s → 30s
- **`PriceCache`** — `ConcurrentHashMap<String, QuoteMessage>` (in-memory, per-ticker)
- **`WebSocketManager`** — отслеживает сессии и их подписки; broadcast только тем клиентам, которые подписаны на пришедший тикер
- WebSocket авторизация: JWT в `Authorization: Bearer` или `?accessToken=`
- Heartbeat: pingPeriodMillis = 30 000

#### 2.8 Обработка ошибок (`StatusPages.kt`)

Единый формат для всех ошибок:
```json
{
  "errorCode": "INSUFFICIENT_FUNDS",
  "message": "Insufficient funds",
  "details": {"required": "2520.0000", "available": "1000.0000"},
  "traceId": "..."
}
```

Коды: `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `INSTRUMENT_NOT_FOUND`, `INSUFFICIENT_FUNDS`, `INSUFFICIENT_POSITION`, `QUOTE_UNAVAILABLE`, `CONFLICT`, `INTERNAL_ERROR`.

#### 2.9 OpenTelemetry

- `KtorServerTelemetry` — автоматическая HTTP-инструментация
- Экспорт в OTLP: `OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318`
- `service.name=ktor-backend`
- Логи в JSON (logstash-logback), поля: `timestamp`, `level`, `service_name`, `message`, `traceId`

---

### Инфраструктура

#### `docker-compose.yml`

| Сервис | Образ | Назначение |
|---|---|---|
| `postgres` | postgres:16 | Транзакционная БД, volume `postgres_data`, healthcheck pg_isready |
| `redis` | redis:7-alpine | Pub/Sub + кэш, AOF everysec, maxmemory 512 МБ, volatile-ttl |
| `redis-exporter` | oliver006/redis_exporter:v1.66.0 | Метрики Redis → порт 9121 |
| `postgres-exporter` | prometheuscommunity/postgres-exporter:v0.16.0 | Метрики PG → порт 9187 |
| `ktor` | ./Dockerfile | Backend-сервис, порт 8080 |

#### `swagger.yaml`

OpenAPI 3.0 — все 12 эндпоинтов задокументированы с request/response схемами, примерами и кодами ошибок.

---

## Исправленные уязвимости (Snyk)

Все уязвимости найдены в **транзитивных зависимостях** Flyway (не в коде приложения).

| ID | Пакет | Версия | CWE / CVE | Статус | Решение |
|---|---|---|---|---|---|
| `SNYK-JAVA-ORGAPACHECOMMONS-6254296` | `commons-compress` | 1.24.0 | CWE-835 / CVE-2024-25710 (Infinite loop) | ✅ Устранена | `force("...commons-compress:1.26.0")` |
| `SNYK-JAVA-ORGAPACHECOMMONS-10734078` | `commons-lang3` | 3.14.0 | CWE-674 / CVE-2025-48924 (Uncontrolled Recursion) | ✅ Устранена | `force("...commons-lang3:3.18.0")` |
| `SNYK-JAVA-COMFASTERXMLJACKSONCORE-15365924` | `jackson-core` | 2.18.3 | CWE-770 (Resource exhaustion) | ✅ Устранена | `force("...jackson-core:2.18.6")` |
| `SNYK-JAVA-COMFASTERXMLJACKSONCORE-15907551` | `jackson-core` | 2.18.6 | CWE-770 (Resource exhaustion) | ⚠️ Отложена | Fix требует jackson-core 2.21.2 (не выпущена). Flyway использует Jackson только для внутренних classpath-файлов, user input не затронут. Задокументировано в `.snyk`, expires 2026-10-25. |

Исправления в `server/build.gradle.kts`:
```kotlin
configurations.all {
    resolutionStrategy {
        force("com.fasterxml.jackson.core:jackson-core:2.18.6")
        force("org.apache.commons:commons-compress:1.26.0")
        force("org.apache.commons:commons-lang3:3.18.0")
    }
}
```

---

## Что осталось (задачи Gendalf)

- [ ] Интеграционные тесты (Testcontainers: PostgreSQL + Redis)
- [ ] Критический путь: register → login → buy → portfolio → sell → transactions
- [ ] Граничные случаи: INSUFFICIENT_FUNDS, INSUFFICIENT_POSITION, QUOTE_UNAVAILABLE
- [ ] Тест параллельных покупок (10 конкурентных запросов → нет отрицательного баланса)
- [ ] WebSocket smoke-тест
- [ ] Обновить `.snyk` когда выйдет jackson-core 2.21.2 / flyway 11.8.1

---

## Структура файлов (ключевые)

```
ktor/
├── docker-compose.yml
├── Dockerfile
├── .env.example
├── .snyk
├── swagger.yaml
├── plan.md
├── gradle/libs.versions.toml
└── server/src/main/
    ├── kotlin/
    │   ├── Application.kt                     ← точка входа модуля
    │   ├── com/example/
    │   │   ├── config/AppConfig.kt
    │   │   ├── db/{DatabaseFactory,Transaction}.kt
    │   │   ├── domain/{Models,Errors}.kt
    │   │   ├── auth/{JwtUtil,PasswordUtil,AuthService,AuthRoutes,...}.kt
    │   │   ├── instruments/{Repository,Routes}.kt
    │   │   ├── portfolio/{Repository,Routes}.kt
    │   │   ├── orders/{Repository,Service,Routes}.kt  ← BUY/SELL логика
    │   │   ├── transactions/{Repository,Routes}.kt
    │   │   ├── quotes/{PriceCache,WebSocketManager,RedisSubscriber,WsHandler}.kt
    │   │   └── health/HealthRoutes.kt
    │   └── plugins/{Security,Monitoring,Serialization,StatusPages,Websockets,OpenTelemetry}.kt
    └── resources/
        ├── application.yaml
        ├── logback.xml
        └── db/migration/{V001__init,V002__indexes,V003__seed}.sql
```

---

## Как запустить

### Вариант 1 — Docker Compose (рекомендуется)

```bash
cp .env.example .env          # выставить JWT_SECRET и POSTGRES_PASSWORD
docker compose up --build     # поднимает postgres + redis + exporters + ktor
curl http://localhost:8080/health/ready
```

### Вариант 2 — Локально (только инфраструктура в Docker)

```bash
docker compose up postgres redis -d
cp .env.example .env          # POSTGRES_HOST=localhost, REDIS_HOST=localhost
export $(grep -v '^#' .env | xargs)
./gradlew :server:run
```

### Порядок инициализации

1. Docker поднимает `postgres` → healthcheck `pg_isready`
2. Docker поднимает `redis` → healthcheck `PING`
3. Ktor стартует: **Flyway запускает миграции V001→V003** автоматически
4. Поднимается HTTP (порт 8080) + Redis Pub/Sub подписка (`quotes.ticks`)
5. `/health/ready` → `{"status":"UP","db":"UP","redis":"UP"}`
