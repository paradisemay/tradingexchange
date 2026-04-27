# Контракты взаимодействия: Go Collector

## 1. C-драйвер → Go

**Транспорт:** файл блочного устройства (sysfs), путь задаётся через `DRIVER_DEVICE_PATH`.

**Формат:** непрерывный бинарный поток структур, Little-Endian:

```c
struct mock {
    char  ticker[8];  // null-terminated C-строка, например "SBER\0\0\0\0"
    float price;      // IEEE 754 single precision (32 бит)
};
// sizeof(struct mock) == 12 байт
```

**Go-представление** (`internal/driver/reader.go`):
```go
type rawQuote struct {
    Ticker [8]byte
    Price  float32
}
```

**Парсинг:**
```go
binary.Read(bytes.NewReader(buf), binary.LittleEndian, &raw)
ticker := cStringToGo(raw.Ticker[:])   // обрезаем нулевые байты
price  := float64(raw.Price)           // расширяем до float64
ts     := time.Now().UnixMilli()       // метка времени — момент чтения
```

**Поведение при ошибках:**
- EOF при закрытии контекста → нормальное завершение (`nil`)
- Канал `out` переполнен → тик дропается с `log.Warn`, чтение не блокируется
- Прочие ошибки чтения → возвращаем `error`, main логирует и завершается

---

## 2. Go → Redis Streams  *(этап 2, ещё не реализован)*

**Транспорт:** Redis Streams, native protocol (`github.com/redis/go-redis/v9`)

**Команда записи:**
```
XADD stream:quotes:v1 MAXLEN ~ 100000 * payload <protobuf-bytes>
```

**Формат payload:** бинарный Protobuf по схеме `quotes.v1.QuoteTick` (`proto/quotes.proto`):
```protobuf
message QuoteTick {
  string ticker       = 1;
  double price        = 2;
  int64  timestamp_ms = 3;
}
```

**Интерфейс** (`internal/publisher/publisher.go`):
```go
type Publisher interface {
    Publish(ctx context.Context, q model.Quote) error
    Close() error
}
```

**Retry:** exponential backoff 100ms → 1s → 5s → 30s.  
**Текущая реализация:** `LogPublisher` — логирует в stdout вместо отправки в Redis.

**Consumer на стороне Ktor:** consumer group `cg:ktor:quotes`.

---

## 3. Go → ClickHouse

**Транспорт:** native TCP, порт 9000 (`github.com/ClickHouse/clickhouse-go/v2`)

**Таблица:**
```sql
CREATE TABLE quotes_raw (
    symbol       String,
    event_time   DateTime64(3),   -- метка из TimestampMs
    price        Float64,
    ingested_at  DateTime64(3)    -- момент вставки
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(event_time)
ORDER BY (symbol, event_time);
```

**Правила батчинга:**
- Размер батча ≤ `CLICKHOUSE_BATCH_SIZE` (по умолчанию 10 000)
- Принудительный flush каждые `CLICKHOUSE_FLUSH_INTERVAL_MS` мс (по умолчанию 200)
- Одиночные INSERT запрещены
- При graceful shutdown — дописать остаток буфера перед выходом
- При временной ошибке insert выполняется retry с backoff от `CLICKHOUSE_RETRY_INITIAL_MS` до `CLICKHOUSE_RETRY_MAX_MS`
- Одна попытка insert ограничена `CLICKHOUSE_INSERT_TIMEOUT_MS`

**Интерфейс** (`internal/batcher/batcher.go`):
```go
type Batcher interface {
    Add(ctx context.Context, q model.Quote) error
    Close(ctx context.Context) error  // flush + close
}
```

**Текущая реализация:** `ClickHouseBatcher` включается через `CLICKHOUSE_ENABLED=true`. Если флаг выключен, используется `LogBatcher` для локального dev-режима.

**Ktor-facing чтение:** детали таблиц, sample SQL и OpenAPI-контракт лежат в `clickhouse/`.

---

## 4. Protobuf-схема

Файл: `proto/quotes.proto` (общий для Go и Ktor).

```protobuf
syntax = "proto3";
package quotes.v1;
option go_package = "github.com/tradingexchange/proto/quotes/v1;quotesv1";

message QuoteTick {
  string ticker       = 1;
  double price        = 2;
  int64  timestamp_ms = 3;
}
```

**Генерация Go-кода:**
```bash
protoc --go_out=internal/proto --go_opt=paths=source_relative proto/quotes.proto
```

**Версионирование:** при несовместимом изменении создаётся `quotes.v2`, `v1` не ломается.
