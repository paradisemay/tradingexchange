// mock-driver имитирует C-драйвер котировок: пишет бесконечный поток
// бинарных структур struct mock { char ticker[8]; float price; } в stdout.
//
// Запуск совместно с коллектором:
//
//	go run ./cmd/mock-driver | DRIVER_DEVICE_PATH=/dev/stdin go run ./cmd/quotes-collector
package main

import (
	"context"
	"encoding/binary"
	"log/slog"
	"math/rand/v2"
	"os"
	"os/signal"
	"strconv"
	"strings"
	"syscall"
	"time"
)

// rawQuote зеркалит C-структуру драйвера.
type rawQuote struct {
	Ticker [8]byte
	Price  float32
}

func main() {
	// Логи — в stderr, чтобы stdout остался чистым бинарным потоком.
	log := slog.New(slog.NewTextHandler(os.Stderr, nil))

	tickers := strings.Split(envOr("MOCK_TICKERS", "SBER,GAZP,YNDX,LKOH,ROSN"), ",")
	intervalMs, _ := strconv.Atoi(envOr("MOCK_INTERVAL_MS", "100"))

	// Стартовые цены: случайное значение в диапазоне 100–1000 для каждого тикера.
	prices := make(map[string]float64, len(tickers))
	for _, t := range tickers {
		prices[t] = 100 + rand.Float64()*900
	}

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	log.Info("mock-driver started", "tickers", tickers, "interval_ms", intervalMs)

	ticker := time.NewTicker(time.Duration(intervalMs) * time.Millisecond)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			log.Info("mock-driver stopped")
			return
		case <-ticker.C:
			for _, name := range tickers {
				// Случайное блуждание цены: ±0.5% за тик.
				prices[name] += (rand.Float64() - 0.5) * 0.01 * prices[name]
				if prices[name] < 1 {
					prices[name] = 1
				}

				var q rawQuote
				copy(q.Ticker[:], name)
				q.Price = float32(prices[name])

				if err := binary.Write(os.Stdout, binary.LittleEndian, q); err != nil {
					log.Error("stdout write error", "err", err)
					return
				}
			}
		}
	}
}

func envOr(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}
