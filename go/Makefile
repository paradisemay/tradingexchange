GO := $(shell which go 2>/dev/null || echo /usr/local/go/bin/go)
DC := docker compose

.PHONY: run run-pipe test cover build clean \
        docker-run docker-test docker-stop docker-run-cdriver

# ── Локальный запуск (без Docker) ───────────────────────────────────────────

## run: mock-driver → именованный пайп → collector (всё локально)
run:
	@mkfifo /tmp/dev-quotes 2>/dev/null || true
	$(GO) run ./cmd/mock-driver > /tmp/dev-quotes & \
	DRIVER_DEVICE_PATH=/tmp/dev-quotes $(GO) run ./cmd/quotes-collector; \
	kill %% 2>/dev/null; rm -f /tmp/dev-quotes

## run-pipe: mock-driver | collector через stdin
run-pipe:
	$(GO) run ./cmd/mock-driver | DRIVER_DEVICE_PATH=/dev/stdin $(GO) run ./cmd/quotes-collector

## test: юнит-тесты с race detector (локально)
test:
	$(GO) test -race -count=1 ./...

## cover: тесты с отчётом о покрытии (локально)
cover:
	$(GO) test -cover ./...

## build: компиляция бинарей в ./bin/
build:
	@mkdir -p bin
	$(GO) build -o bin/quotes-collector ./cmd/quotes-collector
	$(GO) build -o bin/mock-driver ./cmd/mock-driver

# ── Docker ───────────────────────────────────────────────────────────────────

## docker-run: собрать образы и запустить dev-стенд (mock-driver + collector)
docker-run:
	$(DC) up --build

## docker-test: собрать образ и запустить тесты внутри Docker
docker-test:
	$(DC) --profile test run --rm --build test

## docker-stop: остановить и удалить контейнеры + тома
docker-stop:
	$(DC) down -v

## docker-run-cdriver: запустить collector с реальным C-драйвером (без мока)
## Предварительно загрузи модуль: sudo insmod <driver>.ko
docker-run-cdriver:
	$(DC) -f docker-compose.yml -f docker-compose.cdriver.yml up --build collector

clean:
	rm -rf bin/ /tmp/dev-quotes
