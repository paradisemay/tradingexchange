package batcher

import (
	"context"
	"log/slog"

	"github.com/tradingexchange/go-collector/internal/model"
)

// Batcher накапливает котировки и сбрасывает их батчами в ClickHouse.
type Batcher interface {
	Add(ctx context.Context, q model.Quote) error
	// Close сбрасывает остаток буфера и освобождает ресурсы.
	// Вызывается при graceful shutdown после отмены контекста.
	Close(ctx context.Context) error
}

// LogBatcher — временный мок: логирует котировку в stdout вместо записи в ClickHouse.
// Будет заменён на ClickHouseBatcher в этапе 3.
type LogBatcher struct {
	log *slog.Logger
}

func NewLogBatcher(log *slog.Logger) *LogBatcher {
	return &LogBatcher{log: log}
}

func (b *LogBatcher) Add(_ context.Context, q model.Quote) error {
	b.log.Info("[mock] batcher: received quote",
		"ticker", q.Ticker,
		"price", q.Price,
		"timestamp_ms", q.TimestampMs,
	)
	return nil
}

func (b *LogBatcher) Close(_ context.Context) error { return nil }
