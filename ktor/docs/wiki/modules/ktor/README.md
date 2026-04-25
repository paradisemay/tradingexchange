# Модуль: Ktor Backend

**Тимлид:** weebat  
**Саппорт (тесты):** Gendalf  
**Роль в системе:** бизнес-логика и клиентский API брокерского приложения. Единственный потребитель PostgreSQL. Транслирует котировки из Redis в WebSocket мобильным клиентам.

---

## Архитектура

```
routing
  └─ service         ← бизнес-логика, финансовые операции
       └─ repository  ← SQL через prepared statements (Jdbi / JdbcTemplate-подобный DAO)
            ├─ PostgreSQL (HikariCP / JDBC)
            └─ Redis (Pub/Sub, in-memory price cache)
```

Роуты не содержат SQL и финансовой логики. Блокирующие JDBC-операции выполняются на `Dispatchers.IO`.

---

## REST API

| Метод | Путь | Описание |
|---|---|---|
| POST | /api/v1/auth/register | Регистрация |
| POST | /api/v1/auth/login | Вход, выдача токенов |
| POST | /api/v1/auth/refresh | Ротация refresh token |
| POST | /api/v1/auth/logout | Отзыв refresh token |
| GET | /api/v1/me | Профиль текущего пользователя |
| GET | /api/v1/portfolio | Позиции + cash (currentPrice из Redis или null) |
| GET | /api/v1/instruments?query= | Поиск инструментов |
| POST | /api/v1/orders | Создание заявки BUY/SELL |
| GET | /api/v1/orders | История заявок (cursor-пагинация) |
| GET | /api/v1/transactions | История транзакций (cursor-пагинация) |
| GET | /health/live | Liveness probe |
| GET | /health/ready | Readiness probe (db + redis) |
| WS | /api/v1/quotes/ws | WebSocket котировок |

**Формат ошибки:**
```json
{
  "errorCode": "INSUFFICIENT_FUNDS",
  "message": "Not enough cash to execute order",
  "details": {"required": "2520.0000", "available": "1000.0000"},
  "traceId": "..."
}
```

**Коды ошибок:** VALIDATION_ERROR, UNAUTHORIZED, FORBIDDEN, INSTRUMENT_NOT_FOUND, INSUFFICIENT_FUNDS, INSUFFICIENT_POSITION, QUOTE_UNAVAILABLE, CONFLICT, INTERNAL_ERROR

---

## WebSocket: котировки

- Endpoint: GET /api/v1/quotes/ws
- Auth: JWT в заголовке Authorization или ?accessToken= query param
- Клиент → сервер: `{"type":"subscribe","tickers":["SBER","GAZP"]}`
- Сервер → клиент: `{"type":"quote","ticker":"SBER","price":"252.0000","currency":"RUB","timestampMs":1775901000000}`
- Heartbeat: ping/pong каждые 30 секунд
- Redis reconnect backoff: 1s → 2s → 5s → 10s → 30s; метрика redis_reconnect_count

---

## Авторизация

- Access token (JWT): 15 мин (`JWT_ACCESS_TTL_MINUTES=15`)
- Refresh token: 30 дней (`JWT_REFRESH_TTL_DAYS=30`), хранится хэшем в sessions
- Ротация: старый refresh инвалидируется немедленно при /auth/refresh
- Пароли: Argon2id

---

## Контракт с Redis

Подписка на канал `quotes.ticks`. Формат входящего сообщения от Go-сервиса:
```json
{"ticker":"SBER","price":"252.0000","currency":"RUB","timestampMs":1775901000000,"source":"go-quotes"}
```
In-memory кэш последней цены используется для валидации market-заявок и ответа /portfolio. Если цены нет в кэше и last_price IS NULL в instruments → QUOTE_UNAVAILABLE.

---

## OpenTelemetry

Экспорт: `OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318`

**Обязательные атрибуты:** service.name=ktor-backend, http.route, http.status_code, db.system=postgresql, messaging.system=redis

**Метрики:** latency по роутам, 4xx/5xx count, активные WebSocket-сессии, redis_reconnect_count, DB pool usage, длительность транзакций buy/sell

---

## Переменные окружения

```env
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=broker
POSTGRES_USER=broker_app
POSTGRES_PASSWORD=<secret>
POSTGRES_MAX_POOL_SIZE=30
POSTGRES_CONNECTION_TIMEOUT_MS=3000
JWT_SECRET=<secret>
JWT_ACCESS_TTL_MINUTES=15
JWT_REFRESH_TTL_DAYS=30
REDIS_HOST=redis
REDIS_PORT=6379
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318
```

---

## Definition of Done

- [ ] /health/live → 200 {"status":"UP"}
- [ ] /health/ready → 200 {"status":"UP","db":"UP","redis":"UP"}
- [ ] Все REST-эндпоинты и WS реализованы по контракту
- [ ] BUY/SELL атомарны: нет отрицательных балансов при параллельных запросах
- [ ] Тесты Gendalf (Testcontainers): критический путь + граничные случаи
- [ ] OTel трейсы видны в Collector: HTTP → service → PostgreSQL/Redis
- [ ] Graceful shutdown: новые сессии не принимаются, Redis и DB pool освобождены
- [ ] Snyk: нет уязвимостей high/critical в зависимостях
- [ ] swagger.yaml актуален и покрывает все эндпоинты