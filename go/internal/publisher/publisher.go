package publisher

import (
	"context"
	"log/slog"

	"github.com/tradingexchange/go-collector/internal/model"
)

// Publisher отправляет котировки в брокер сообщений (Redis Streams).
// Аналог Java: интерфейс без явного implements — любой тип с методами Publish/Close его реализует.
type Publisher interface {
	Publish(ctx context.Context, q model.Quote) error
	Close() error
}

// LogPublisher — временный мок: логирует котировку в stdout вместо отправки в Redis.
// Будет заменён на RedisPublisher в этапе 2.
type LogPublisher struct {
	log *slog.Logger
}

func NewLogPublisher(log *slog.Logger) *LogPublisher {
	return &LogPublisher{log: log}
}

func (p *LogPublisher) Publish(_ context.Context, q model.Quote) error {
	p.log.Info("[mock] publisher: received quote",
		"ticker", q.Ticker,
		"price", q.Price,
		"timestamp_ms", q.TimestampMs,
	)
	return nil
}

func (p *LogPublisher) Close() error { return nil }
