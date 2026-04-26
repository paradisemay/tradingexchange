# CLAUDE.md — правила работы над Go-модулем (quotes-collector)

## Роль модуля в системе

Микросервис на Go — **центральный конвейер данных** торговой экосистемы. Он:
1. Непрерывно считывает поток котировок из C-драйвера (блочное устройство sysfs)
2. Публикует горячие тики в **Redis Streams** (`stream:quotes:v1`) в формате Protobuf для Ktor и 10 000 клиентов в реальном времени
3. Батчами пишет исторические данные в **ClickHouse** (≤10 000 записей или flush каждые 200 мс)

Модуль **не** работает с PostgreSQL напрямую. Ответственность за бизнес-логику лежит на Ktor.

---

## Архитектура модуля

```
cmd/quotes-collector/
  main.go               ← точка входа, DI, signal handling (SIGINT/SIGTERM)

internal/
  config/
    config.go           ← вся конфигурация через os.Getenv; паника при отсутствии обязательных переменных
  model/
    quote.go            ← доменная структура Quote {Ticker, Price, EventTime, IngestedAt}
  driver/
    reader.go           ← бинарное чтение struct mock {char ticker[8]; float price} из device-файла
  pipeline/
    fanout.go           ← fan-out: один входной канал → два исходящих (redis, clickhouse)
  publisher/
    redis.go            ← XADD stream:quotes:v1 MAXLEN ~ 100000, payload = protobuf bytes
  batcher/
    clickhouse.go       ← буфер ≤10 000 записей или ticker 200 мс, native TCP insert
  telemetry/
    otel.go             ← OpenTelemetry SDK: трейсы, метрики (OTLP → otel-collector:4317)

proto/
  quotes/v1/
    quote.proto         ← единая Protobuf-схема (версия v1), общая с Ktor

Dockerfile              ← multi-stage: builder (golang:1.23-alpine) → runtime (alpine)
docker-compose.yml      ← поднимает весь модуль + зависимости (redis, clickhouse) одной командой
.env.example            ← образец всех переменных окружения (без реальных значений)
plan.md                 ← текущий план декомпозиции задач
```

---

## Планирование и процесс

**Перед началом любой задачи:**
1. Декомпозируй задачу на подзадачи
2. Запиши план в `go/plan.md` (создай, если не существует) и обновляй статус по ходу
3. Каждый завершённый этап — отдельный коммит (см. «Коммиты» ниже)
4. Параллельно с кодом обновляй wiki в `go/docs/wiki/`

**Структура wiki:**
```
go/docs/wiki/
  architecture.md       ← общая схема модуля и потоков данных
  contracts.md          ← контракты с соседями: C-driver, Redis, ClickHouse, proto-схема
  config.md             ← все переменные окружения с описанием и дефолтами
  runbook.md            ← как запустить локально, как запустить тесты, как отладить
  adr/                  ← Architecture Decision Records (одно решение — один файл)
```

---

## Переменные окружения — строгий запрет на хардкод

Никогда не хардкодь токены, пароли, адреса хостов и порты. Всё — через ENV:

```
# C-driver
DRIVER_DEVICE_PATH=/dev/quotes          # путь к файлу блочного устройства

# Redis
REDIS_ADDR=localhost:6379
REDIS_PASSWORD=
REDIS_STREAM=stream:quotes:v1
REDIS_STREAM_MAXLEN=100000

# ClickHouse
CLICKHOUSE_ADDR=localhost:9000
CLICKHOUSE_DATABASE=trading
CLICKHOUSE_USER=default
CLICKHOUSE_PASSWORD=
CLICKHOUSE_TABLE=quotes_raw
CLICKHOUSE_BATCH_SIZE=10000            # макс. размер батча
CLICKHOUSE_FLUSH_INTERVAL_MS=200       # интервал принудительного flush

# OpenTelemetry
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_SERVICE_NAME=go-quotes-collector

# Прочее
LOG_LEVEL=info
```

При добавлении новой переменной — **обязательно** обновить `.env.example`.

---

## Ключевые технические правила

### Конкурентность
- Чтение из драйвера — отдельная горутина; никогда не блокируй её записью в Redis или CH
- Fan-out через буферизованные каналы; при переполнении логируй и дропай (не паникуй)
- Жизненный цикл всех горутин управляется через `context.Context` с `cancel`
- Graceful shutdown: по SIGINT/SIGTERM отменяем контекст, ждём завершения всех горутин через `sync.WaitGroup`, затем дописываем остаток буфера в CH

### Redis Streams
- Команда записи: `XADD stream:quotes:v1 MAXLEN ~ 100000 * payload <protobuf-bytes>`
- Payload — бинарный Protobuf по схеме `quotes.v1.QuoteTick`
- При временной ошибке Redis — retry с exponential backoff (100 мс → 1 с → 5 с → 30 с)
- Не теряй тики при retry: используй буфер и повторную попытку

### ClickHouse
- Библиотека: `github.com/ClickHouse/clickhouse-go/v2`, native TCP (порт 9000)
- Вставка только батчами: ≤10 000 записей **или** flush каждые 200 мс — что наступит раньше
- Одиночные INSERT запрещены
- При graceful shutdown — дописать остаток буфера перед выходом
- При ошибке вставки — retry с backoff, логировать потерянные записи отдельно

### Protobuf
- Схема живёт в `proto/quotes/v1/quote.proto` (общий каталог с Ktor и другими сервисами)
- Генерировать Go-код командой `buf generate` или `protoc`; сгенерированные файлы коммитить
- При несовместимом изменении схемы — создавать новую версию `quotes.v2`, не ломать v1

### Обработка ошибок
- Все ошибки логировать с контекстом: `ticker`, `timestamp`, `component`
- Не используй `panic` в рабочем коде (только в `main` при невалидной конфигурации)
- Структурированные JSON-логи: `{"level":"error","component":"redis-publisher","ticker":"SBER","err":"..."}`

---

## Тесты

- Покрытие юнит-тестами: **≥ 80%** (проверять через `go test -cover ./...`)
- Таблица тестируемых компонентов:

| Компонент | Тип теста | Приоритет |
|---|---|---|
| `driver.Reader` | unit (mock-файл через `io.Reader`) | высокий |
| `pipeline.FanOut` | unit (каналы, дроп при переполнении) | высокий |
| `publisher.Redis` | unit (mock Redis client) + integration | высокий |
| `batcher.ClickHouse` | unit (таймер + размер батча) + integration | высокий |
| `config.Load` | unit (env vars) | средний |
| `model.Quote` | unit (валидация) | низкий |

- Интеграционные тесты запускать с `testcontainers-go` (Redis, ClickHouse)
- Тесты пишутся **параллельно** с кодом, не после
- Запуск: `go test ./... -race -count=1`

---

## Docker

- **Один `docker-compose.yml` поднимает весь модуль** со всеми зависимостями (Redis, ClickHouse)
- Multi-stage Dockerfile: `golang:1.23-alpine` для сборки, `alpine` для runtime
- Бинарь компилируется с флагами: `CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w"`
- Healthcheck: проверка доступности Redis и ClickHouse перед стартом pipeline (`depends_on` с `condition: service_healthy`)
- Переменные окружения передаются через `env_file: .env`, не хардкодятся в `docker-compose.yml`

---

## Коммиты (Conventional Commits)

Каждый этап декомпозиции — **отдельный коммит**. Монолитные коммиты запрещены.

```
feat:     новая функциональность (reader, publisher, batcher, ...)
fix:      исправление ошибки
chore:    зависимости, Dockerfile, конфигурация
test:     тесты
docs:     wiki, README, комментарии к архитектуре
refactor: рефакторинг без изменения поведения
proto:    изменения .proto-схем
```

Формат: `feat(batcher): add ClickHouse batch flush on 200ms ticker`

---

## Контракты с соседними модулями

### C-драйвер → Go
- Чтение из файла блочного устройства (путь задаётся через `DRIVER_DEVICE_PATH`)
- Формат: бинарный поток структур `struct mock { char ticker[8]; float price; }`
- Парсить с помощью `encoding/binary`, LittleEndian (уточнить с командой C-драйвера)
- ticker — null-terminated C string длиной 8 байт; обрезать нули перед использованием

### Go → Redis
- Команда: `XADD stream:quotes:v1 MAXLEN ~ 100000 * payload <bytes>`
- Payload: бинарный Protobuf `quotes.v1.QuoteTick`
- Consumer group на стороне Ktor: `cg:ktor:quotes`

### Go → ClickHouse
- Таблица: `quotes_raw(symbol String, event_time DateTime64(3), price Float64, ingested_at DateTime64(3))`
- Протокол: native TCP (порт 9000)
- Батчинг: ≤10 000 записей или flush каждые 200 мс
