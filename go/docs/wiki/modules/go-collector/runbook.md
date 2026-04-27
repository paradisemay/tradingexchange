# Runbook: Go Quotes Collector

## Предварительные требования

- Go 1.26+ (`go version`)
- Docker + Docker Compose (для docker-режима)
- Python 3 (опционально, для ручной генерации тестовых данных)

---

## Локальный запуск (без Docker)

### Вариант 1: через именованный пайп (рекомендуется)
```bash
make run
```
Создаёт `/tmp/dev-quotes`, запускает `mock-driver` в фоне, затем коллектор. Остановка — `Ctrl+C`.

### Вариант 2: через stdin
```bash
make run-pipe
```
Эквивалент: `go run ./cmd/mock-driver | DRIVER_DEVICE_PATH=/dev/stdin go run ./cmd/quotes-collector`

### Вариант 3: два терминала (ручной контроль)
```bash
# Терминал 1 — запуск коллектора
mkfifo /tmp/dev-quotes
DRIVER_DEVICE_PATH=/tmp/dev-quotes go run ./cmd/quotes-collector

# Терминал 2 — запуск генератора
MOCK_TICKERS=SBER,GAZP MOCK_INTERVAL_MS=200 go run ./cmd/mock-driver > /tmp/dev-quotes
```

---

## Docker-режим

### Dev-стенд (mock-driver + collector)
```bash
make docker-run          # сборка + запуск
make docker-stop         # остановка + удаление томов
```

### Только тесты
```bash
make docker-test         # пересобирает образ tester и прогоняет go test -race ./...
```

### С реальным C-драйвером
```bash
# 1. Загрузить модуль ядра на хосте:
sudo insmod /path/to/quotes_driver.ko

# 2. Убедиться, что устройство появилось:
ls -la /dev/quotes

# 3. Запустить коллектор с монтированием устройства:
make docker-run-cdriver
```

---

## Тесты

```bash
make test       # локально
make test-race  # локально с race detector, нужен CGO/C compiler
make test-integration  # ClickHouse через testcontainers-go, нужен Docker
make cover      # локально, с отчётом о покрытии по пакетам
make docker-test  # в Docker-контейнере
```

Запуск конкретного теста:
```bash
/usr/local/go/bin/go test -v -run TestReader_StopsOnContextCancel ./internal/driver/
```

---

## Отладка

### Посмотреть что пишет mock-driver в бинаре
```bash
go run ./cmd/mock-driver | xxd | head -5
# 00000000: 5342 4552 0000 0000 0079 7c43  SBER.....y|C
#           [--- ticker 8B --] [price 4B ]
```

### Изменить тикеры или частоту mock-driver
```bash
MOCK_TICKERS=AAPL,TSLA MOCK_INTERVAL_MS=500 go run ./cmd/mock-driver > /tmp/dev-quotes &
DRIVER_DEVICE_PATH=/tmp/dev-quotes go run ./cmd/quotes-collector
```

### Включить debug-логирование
```bash
LOG_LEVEL=debug DRIVER_DEVICE_PATH=... go run ./cmd/quotes-collector
```

### Включить запись в ClickHouse

Сначала подними ClickHouse из соседнего модуля:

```powershell
cd C:\Users\yarik\GolandProjects\tradingexchange\clickhouse
Copy-Item .env.example .env
docker compose up -d
```

Затем запусти Go локально с тем же паролем:

```powershell
cd C:\Users\yarik\GolandProjects\tradingexchange\go
$env:DRIVER_DEVICE_PATH="/dev/stdin"
$env:CLICKHOUSE_ENABLED="true"
$env:CLICKHOUSE_ADDR="localhost:9000"
$env:CLICKHOUSE_DATABASE="trading"
$env:CLICKHOUSE_USER="trading_app"
$env:CLICKHOUSE_PASSWORD="<local password from clickhouse/.env>"
go run ./cmd/mock-driver | go run ./cmd/quotes-collector
```

### Проверить покрытие тестами с HTML-отчётом
```bash
/usr/local/go/bin/go test -coverprofile=coverage.out ./...
/usr/local/go/bin/go tool cover -html=coverage.out
```

---

## Ожидаемый вывод при нормальной работе

```json
{"time":"2026-04-26T10:00:00Z","level":"INFO","msg":"quotes-collector started","device":"/shared/quotes"}
{"time":"2026-04-26T10:00:00Z","level":"INFO","msg":"[mock] publisher: received quote","ticker":"SBER","price":252.4817,"timestamp_ms":1745661600000}
{"time":"2026-04-26T10:00:00Z","level":"INFO","msg":"[mock] batcher: received quote","ticker":"SBER","price":252.4817,"timestamp_ms":1745661600000}
{"time":"2026-04-26T10:00:00Z","level":"INFO","msg":"[mock] publisher: received quote","ticker":"GAZP","price":187.1203,"timestamp_ms":1745661600001}
...
```

При `Ctrl+C`:
```json
{"time":"...","level":"INFO","msg":"driver: stopped"}
{"time":"...","level":"INFO","msg":"shutdown complete"}
```
