# Модуль: PostgreSQL

**Тимлид:** weebat  
**Роль в системе:** главная транзакционная БД брокерской системы. Хранит только транзакционные данные: пользователей, счета, портфели, заявки, транзакции. Исторические котировки — в ClickHouse; текущие цены — в Redis.

---

## ER-модель

| Таблица | Назначение |
|---|---|
| users | Профили пользователей, роли (CLIENT / ADMIN) |
| accounts | Денежные счета по валютам, cash_balance + reserved_balance |
| instruments | Справочник инструментов (ticker, lot_size, last_price) |
| portfolio_positions | Текущие позиции: (user_id, ticker) → quantity, avg_price |
| orders | Торговые заявки: BUY/SELL, MARKET/LIMIT, статус FILLED/REJECTED |
| transactions | Финансовая история: DEPOSIT, WITHDRAW, BUY, SELL, FEE |
| sessions | Refresh-токены: hash, expires_at, revoked_at |

Все денежные поля: `numeric(20,4)`. Количество бумаг: `numeric(20,8)`. `float`/`double` запрещены.

---

## Индексы

```sql
idx_orders_user_created_at    ON orders(user_id, created_at DESC)
idx_orders_status_created_at  ON orders(status, created_at)
idx_transactions_user_created ON transactions(user_id, created_at DESC)
idx_positions_user            ON portfolio_positions(user_id)
idx_sessions_user_active      ON sessions(user_id, expires_at) WHERE revoked_at IS NULL
```

---

## Транзакционная логика

**Порядок блокировок (обязателен для исключения deadlock):**
1. `SELECT * FROM accounts WHERE user_id=:u AND currency=:c FOR UPDATE`
2. `SELECT * FROM portfolio_positions WHERE user_id=:u AND ticker=:t FOR UPDATE`

**BUY:** проверка cash_balance → INSERT orders(FILLED) → UPDATE accounts (cash−) → UPSERT portfolio_positions → INSERT transactions(BUY)

**SELL:** проверка quantity → UPDATE portfolio_positions (quantity−) → UPDATE accounts (cash+) → INSERT orders(FILLED) + transactions(SELL)

При любой ошибке — полный rollback. Уровень изоляции: READ COMMITTED с row-level locks.

---

## Миграции (Flyway)

```
db/migration/
  V001__init.sql      — создание всех таблиц
  V002__indexes.sql   — индексы
```

Запускаются Ktor автоматически при старте, до поднятия HTTP. Ручное изменение схемы в контейнере запрещено. Только forward-only миграции.

---

## Роли БД

| Роль | Права |
|---|---|
| broker_app | SELECT / INSERT / UPDATE / DELETE на прикладные таблицы |
| broker_migration | DDL (для Flyway) |

---

## Docker

```yaml
postgres:
  image: postgres:16
  volumes:
    - postgres_data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD", "pg_isready", "-U", "broker_app"]
  # prod: внешний порт не публикуется
```

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
```

---

## Definition of Done

- [ ] Контейнер поднимается, pg_isready проходит
- [ ] Flyway накатывает миграции на чистую базу без ошибок
- [ ] Все таблицы имеют PK, FK, check-constraints, индексы
- [ ] Тест конкурентной покупки: нет отрицательного cash_balance
- [ ] Тест конкурентной продажи: нет отрицательного quantity
- [ ] postgres-exporter передаёт метрики в телеметрию