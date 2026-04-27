package pipeline

import (
	"context"
	"errors"
	"io"
	"log/slog"
	"testing"

	"github.com/tradingexchange/go-collector/internal/model"
)

func TestRunFansOutQuotes(t *testing.T) {
	in := make(chan model.Quote, 2)
	in <- model.Quote{Ticker: "SBER", Price: 1}
	in <- model.Quote{Ticker: "GAZP", Price: 2}
	close(in)

	pub := &fakePublisher{}
	bat := &fakeBatcher{}

	Run(context.Background(), in, pub, bat, slog.New(slog.NewTextHandler(io.Discard, nil)))

	if len(pub.quotes) != 2 {
		t.Fatalf("expected 2 published quotes, got %d", len(pub.quotes))
	}
	if len(bat.quotes) != 2 {
		t.Fatalf("expected 2 batched quotes, got %d", len(bat.quotes))
	}
}

func TestRunContinuesAfterSinkErrors(t *testing.T) {
	in := make(chan model.Quote, 1)
	in <- model.Quote{Ticker: "SBER", Price: 1}
	close(in)

	pub := &fakePublisher{err: errors.New("publish failed")}
	bat := &fakeBatcher{err: errors.New("batch failed")}

	Run(context.Background(), in, pub, bat, slog.New(slog.NewTextHandler(io.Discard, nil)))

	if len(pub.quotes) != 1 || len(bat.quotes) != 1 {
		t.Fatalf("expected both sinks to be called")
	}
}

type fakePublisher struct {
	quotes []model.Quote
	err    error
}

func (f *fakePublisher) Publish(_ context.Context, q model.Quote) error {
	f.quotes = append(f.quotes, q)
	return f.err
}

func (f *fakePublisher) Close() error { return nil }

type fakeBatcher struct {
	quotes []model.Quote
	err    error
}

func (f *fakeBatcher) Add(_ context.Context, q model.Quote) error {
	f.quotes = append(f.quotes, q)
	return f.err
}

func (f *fakeBatcher) Close(context.Context) error { return nil }
