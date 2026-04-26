# syntax=docker/dockerfile:1

# ─── Stage 1: зависимости ────────────────────────────────────────────────────
FROM golang:1.23-alpine AS deps
WORKDIR /app
COPY go.mod ./
RUN go mod download

# ─── Stage 2: сборка бинарей ─────────────────────────────────────────────────
FROM deps AS builder
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" \
      -o /bin/quotes-collector ./cmd/quotes-collector \
 && CGO_ENABLED=0 GOOS=linux go build -ldflags="-s -w" \
      -o /bin/mock-driver ./cmd/mock-driver

# ─── Stage 3: прогон тестов (используется только в CI / make docker-test) ────
# golang:1.23-alpine не содержит gcc, а флаг -race требует CGO.
# Ставим минимальный набор инструментов компилятора.
FROM deps AS tester
RUN apk add --no-cache gcc musl-dev
COPY . .
CMD ["go", "test", "-v", "-race", "-count=1", "./..."]

# ─── Stage 4: минимальный runtime — коллектор ────────────────────────────────
FROM alpine:3.21 AS collector
COPY --from=builder /bin/quotes-collector /app/quotes-collector
# sh нужен для entrypoint-скрипта в docker-compose (ожидание FIFO).
ENTRYPOINT ["/app/quotes-collector"]

# ─── Stage 5: минимальный runtime — мок C-драйвера (только для разработки) ───
FROM alpine:3.21 AS mock-driver
COPY --from=builder /bin/mock-driver /app/mock-driver
ENTRYPOINT ["/app/mock-driver"]
