package pipeline

import (
	"context"
	"log/slog"

	"github.com/tradingexchange/go-collector/internal/batcher"
	"github.com/tradingexchange/go-collector/internal/model"
	"github.com/tradingexchange/go-collector/internal/publisher"
)

// Run читает котировки из in и передаёт каждую в pub и bat.
// Завершается когда канал in закрыт (драйвер остановился) или ctx отменён.
//
// Аналог Java: цикл обработки очереди BlockingQueue в отдельном потоке,
// который читает до сигнала shutdown.
func Run(ctx context.Context, in <-chan model.Quote, pub publisher.Publisher, bat batcher.Batcher, log *slog.Logger) {
	for {
		select {
		case q, ok := <-in:
			if !ok {
				// Канал закрыт — драйвер завершил работу.
				return
			}
			if err := pub.Publish(ctx, q); err != nil {
				log.Error("pipeline: publish error", "ticker", q.Ticker, "err", err)
			}
			if err := bat.Add(ctx, q); err != nil {
				log.Error("pipeline: batcher error", "ticker", q.Ticker, "err", err)
			}
		case <-ctx.Done():
			return
		}
	}
}
