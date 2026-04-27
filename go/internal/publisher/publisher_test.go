package publisher

import (
	"context"
	"io"
	"log/slog"
	"testing"

	"github.com/tradingexchange/go-collector/internal/model"
)

func TestLogPublisher(t *testing.T) {
	p := NewLogPublisher(slog.New(slog.NewTextHandler(io.Discard, nil)))
	if err := p.Publish(context.Background(), model.Quote{Ticker: "SBER", Price: 1}); err != nil {
		t.Fatal(err)
	}
	if err := p.Close(); err != nil {
		t.Fatal(err)
	}
}
