# Модуль: Go Quotes Collector

**Тимлид:** girneos  
**Саппорт:** ttemmaaa, amsterdam121  
**Роль в системе:** центральный конвейер данных. Читает поток котировок из C-драйвера (блочное устройство) и маршрутизирует их в двух направлениях: горячий поток в Redis Streams (для Ktor и 10 000 клиентов в реальном времени) и исторические данные в ClickHouse (для построения графиков).

---

## Архитектура

```
[C-драйвер /dev/quotes]
        │
        │  бинарное чтение 12 байт
        │  struct mock { char ticker[8]; float price; }
        ▼
  driver.Reader           ← горутина #1
  (internal/driver)
        │
        │  model.Quote{Ticker, Price, TimestampMs}
        ▼
  chan model.Quote         ← буфер 1000, drop при переполнении
        │
  pipeline.Run            ← горутина #2
  (internal/pipeline)
        │
        ├──▶ publisher.Publish(q)    → Redis Streams   [этап 2]
        └──▶ batcher.Add(q)         → ClickHouse batch [этап 3]
```

Все горутины управляются через `context.Context` — при SIGINT/SIGTERM контекст отменяется, горутины завершаются, `sync.WaitGroup` ожидает их, батчер сбрасывает буфер.

---

## Структура пакетов

```
cmd/quotes-collector/main.go    ← точка входа: DI, сигналы, WaitGroup
cmd/mock-driver/main.go         ← симулятор C-драйвера (только dev)

internal/
  config/config.go    ← загрузка ENV в struct Config; паника при отсутствии обязательных
  model/quote.go      ← Quote{Ticker string, Price float64, TimestampMs int64}
  driver/
    reader.go         ← Reader: Run() открывает файл, readFrom() парсит бинарный поток
    reader_test.go    ← покрытие 85.2%: парсинг, ctx cancel, drop при полном канале
  pipeline/
    fanout.go         ← Run(): читает из канала, вызывает pub и bat
  publisher/
    publisher.go      ← интерфейс Publisher + LogPublisher (мок → stdout)
  batcher/
    batcher.go        ← интерфейс Batcher + LogBatcher (мок → stdout)
```

---

## Переменные окружения

| Переменная | Обязательная | По умолчанию | Описание |
|---|---|---|---|
| `DRIVER_DEVICE_PATH` | **да** | — | Путь к файлу C-драйвера (например `/dev/quotes`) |
| `LOG_LEVEL` | нет | `info` | Уровень логирования: debug / info / warn / error |
| `MOCK_TICKERS` | нет | `SBER,GAZP,YNDX,LKOH,ROSN` | Тикеры mock-driver (только dev) |
| `MOCK_INTERVAL_MS` | нет | `100` | Интервал генерации тика mock-driver, мс (только dev) |

Переменные Redis и ClickHouse добавятся в этапах 2–3. Шаблон: `.env.example`.

---

## Docker

### Образы (Dockerfile, multi-stage)

| Stage | База | Назначение |
|---|---|---|
| `deps` | golang:1.23-alpine | кэш `go mod download` |
| `builder` | deps | компиляция бинарей |
| `tester` | deps + gcc/musl-dev | `go test -race ./...` |
| `collector` | alpine:3.21 | production-образ коллектора |
| `mock-driver` | alpine:3.21 | dev-образ симулятора драйвера |

### docker-compose.yml — dev-стенд

```
mock-driver ──FIFO /shared/quotes──▶ collector
```

`mock-driver` создаёт именованный пайп и пишет в него. `collector` ждёт появления пайпа и читает из него. Механизм FIFO гарантирует синхронизацию через OS-семантику `open()`.

### docker-compose.cdriver.yml — override для реального C-драйвера

```yaml
# Монтирует устройство хоста в контейнер. mock-driver отключается.
devices:
  - /dev/quotes:/dev/quotes
```

Использование: `docker compose -f docker-compose.yml -f docker-compose.cdriver.yml up collector`

---

## Тесты

| Тест | Файл | Что проверяет |
|---|---|---|
| `TestCStringToGo` | driver/reader_test.go | конвертация C null-terminated string → Go string |
| `TestReader_ParsesBinaryStructs` | driver/reader_test.go | бинарный парсинг rawQuote → model.Quote + timestamp |
| `TestReader_Run` | driver/reader_test.go | публичный метод Run с временным файлом |
| `TestReader_StopsOnContextCancel` | driver/reader_test.go | graceful shutdown через io.Pipe + ctx cancel |
| `TestReader_DropsWhenChannelFull` | driver/reader_test.go | drop тиков при полном канале, не блокировка |

**Покрытие:** `internal/driver` — 85.2%

Запуск: `make test` (локально) или `make docker-test` (в Docker с race detector).

---

## Definition of Done

- [x] Бинарное чтение структур из C-драйвера без аллокаций на тик
- [x] Graceful shutdown: ctx cancel → горутины завершаются → батчер сбрасывает буфер
- [x] Drop тиков при переполнении канала (не блокировка чтения из устройства)
- [x] Покрытие тестами ≥ 80% для driver-пакета
- [x] Запуск одной командой: `make docker-run` (без реального C-драйвера)
- [x] Переключение на реальный C-драйвер: `make docker-run-cdriver` (без изменения кода)
- [ ] Redis publisher: XADD stream:quotes:v1, payload=Protobuf (этап 2)
- [ ] ClickHouse batcher: батч ≤10k / flush 200ms, graceful flush (этап 3)
- [ ] Интеграционные тесты через testcontainers-go (этапы 2–3)
