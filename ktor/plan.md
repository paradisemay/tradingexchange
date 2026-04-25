# plan.md — Брокерский модульный монолит

> Обновляй этот файл после завершения каждой подзадачи. Статусы: [ ] todo · [x] done · [~] in progress

---

## Модуль 1: PostgreSQL

### 1.1 Схема и миграции
- [x] Написать V001__init.sql — создание таблиц users, accounts, instruments, portfolio_positions, orders, transactions, sessions
- [x] Написать V002__indexes.sql — все индексы из SRS (idx_orders_user_created_at и др.)
- [x] Подготовить seed-данные: тестовые инструменты (SBER, GAZP, YNDX, LKOH, GMKN)
- [ ] Создать роли broker_app (DML) и broker_migration (DDL) с минимальными правами (production concern, в dev используется один пользователь)
- [ ] Проверить: Flyway накатывает миграции на чистую базу без ошибок

### 1.2 Docker
- [x] Написать docker-compose сервис postgres:16 с volume postgres_data
- [x] Настроить healthcheck pg_isready
- [x] Внешний порт 5432 (для prod — закрывается)
- [x] Добавить переменные в .env.example: POSTGRES_HOST, POSTGRES_PORT, POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD

### 1.3 Валидация схемы
- [x] Check-constraints (cash_balance >= 0, quantity >= 0, lot_size > 0) — в V001__init.sql
- [x] Foreign keys: accounts→users, orders→users, orders→instruments, и т.д.
- [ ] Тест конкурентной покупки: cash_balance не уходит в минус при 10 параллельных запросах
- [ ] Тест конкурентной продажи: quantity в portfolio_positions не уходит в минус

### 1.4 Наблюдаемость
- [x] Подключить postgres-exporter (в docker-compose.yml)
- [ ] Убедиться, что метрики connections / deadlocks / slow_queries / table_sizes доступны телеметрии

---

## Модуль 2: Ktor

### 2.1 Каркас приложения
- [x] Gradle-проект с Ktor (Netty), kotlinx.serialization
- [x] Настроить слои: routing → service → repository → database/redis
- [x] Подключить HikariCP, настроить через ENV (POSTGRES_MAX_POOL_SIZE=30, POSTGRES_CONNECTION_TIMEOUT_MS=3000)
- [x] Подключить Flyway — запуск миграций до поднятия HTTP
- [x] Реализовать /health/live и /health/ready
- [x] Добавить все переменные в .env.example: JWT_SECRET, JWT_ACCESS_TTL_MINUTES, JWT_REFRESH_TTL_DAYS, REDIS_HOST, REDIS_PORT, OTEL_EXPORTER_OTLP_ENDPOINT

### 2.2 Авторизация
- [x] POST /api/v1/auth/register — хэш пароля Argon2id, запись в users + sessions
- [x] POST /api/v1/auth/login — проверка пароля, выдача JWT access + refresh
- [x] POST /api/v1/auth/refresh — ротация refresh token (старый инвалидируется)
- [x] POST /api/v1/auth/logout — revoke refresh token (revoked_at)
- [x] JWT middleware: проверка подписи, expiry, извлечение userId
- [ ] Тесты: login, refresh rotation, использование отозванного токена → 401

### 2.3 REST API
- [x] GET /api/v1/me
- [x] GET /api/v1/portfolio (currentPrice из Redis in-memory cache, null если нет)
- [x] GET /api/v1/instruments?query=
- [x] POST /api/v1/orders — market/limit заявка BUY/SELL
- [x] GET /api/v1/orders?limit=50&cursor= — cursor-пагинация по created_at
- [x] GET /api/v1/transactions?limit=50&cursor=
- [x] Единый обработчик ошибок: errorCode + message + details + traceId

### 2.4 Бизнес-логика покупки / продажи
- [x] BUY: SELECT FOR UPDATE accounts → portfolio_positions, проверка cash_balance, INSERT orders(FILLED), UPDATE accounts, UPSERT portfolio_positions, INSERT transactions(BUY) — всё в одной транзакции
- [x] SELL: SELECT FOR UPDATE accounts → portfolio_positions, проверка quantity, UPDATE portfolio_positions, UPDATE accounts (cash+), INSERT orders(FILLED) + transactions(SELL)
- [x] При quantity=0 после продажи: avg_price обнуляется, строка остаётся
- [x] Возврат QUOTE_UNAVAILABLE если цена в Redis-кэше отсутствует и last_price IS NULL
- [ ] Тест параллельных BUY: 10 goroutine одновременно → ни одного отрицательного баланса

### 2.5 WebSocket котировок
- [x] GET /api/v1/quotes/ws — JWT-авторизация через header или ?accessToken=
- [x] Подписка на Redis Pub/Sub канал quotes.ticks
- [x] Обработка сообщений subscribe/unsubscribe от клиента
- [x] Рассылка тольк�� по тикерам подписки клиента
- [x] Heartbeat ping/pong каждые 30 секунд (через WebSockets pingPeriodMillis)
- [x] Reconnect к Redis: backoff 1→2→5→10→30s, счётчик reconnectCount
- [x] Graceful shutdown: закрыть Redis-подписку, дождаться завершения корутин

### 2.6 OpenTelemetry
- [x] Подключить OpenTelemetry SDK (KtorServerTelemetry)
- [x] HTTP-инструментация Ktor (latency, 4xx/5xx по роутам)
- [ ] Ручные spans: PostgreSQL transaction (buy/sell duration), Redis pub/sub
- [x] Атрибуты: service.name=ktor-backend
- [ ] Метрика: активные WebSocket-сессии (wsManager.activeCount()), redis_reconnect_count, DB pool usage

### 2.7 Тесты (Gendalf)
- [ ] Поднять Testcontainers: PostgreSQL + Redis
- [ ] Интеграционный тест критического пути: register → login → buy → portfolio → sell → transactions
- [ ] Граничные случаи: INSUFFICIENT_FUNDS, INSUFFICIENT_POSITION, QUOTE_UNAVAILABLE
- [ ] Параллельные покупки: конкурентные запросы одного пользователя не дают отрицательного баланса
- [ ] WebSocket: клиент подписывается, получает тики, отписывается
- [ ] Smoke-тест: 100 / 1000 клиентов одновременно

### 2.8 Swagger
- [x] Сгенерировать swagger.yaml со всеми эндпоинтами (OpenAPI 3.0)
- [ ] Обновить при каждом изменении API-контракта

---

## Интеграция и финальная проверка
- [x] docker-compose up поднимает postgres + ktor без ошибок (конфиг готов)
- [x] Все Flyway-миграции накатываются автоматически (при старте Ktor)
- [x] /health/ready возвращает {"status":"UP","db":"UP","redis":"UP"}
- [ ] Трейсы и метрики видны в Collector / Grafana (нужен OTel Collector)
- [ ] Snyk: нет уязвимостей severity high/critical в зависимостях (требует `snyk auth`)
- [ ] Все тесты Gendalf проходят в CI
