package driver

import (
	"bytes"
	"context"
	"encoding/binary"
	"io"
	"log/slog"
	"os"
	"testing"

	"github.com/tradingexchange/go-collector/internal/model"
)

func TestCStringToGo(t *testing.T) {
	tests := []struct {
		input [8]byte
		want  string
	}{
		{[8]byte{'S', 'B', 'E', 'R', 0, 0, 0, 0}, "SBER"},
		{[8]byte{'G', 'A', 'Z', 'P', 'R', 'M', 0, 0}, "GAZPRM"},
		{[8]byte{'A', 'A', 'P', 'L', 'U', 'S', 'D', 'X'}, "AAPLUSDX"}, // без нуля — берём все 8
		{[8]byte{}, ""},
	}
	for _, tc := range tests {
		got := cStringToGo(tc.input[:])
		if got != tc.want {
			t.Errorf("cStringToGo(%v) = %q, want %q", tc.input, got, tc.want)
		}
	}
}

func TestReader_ParsesBinaryStructs(t *testing.T) {
	buf := &bytes.Buffer{}
	structs := []rawQuote{
		{Ticker: [8]byte{'S', 'B', 'E', 'R', 0}, Price: 252.5},
		{Ticker: [8]byte{'G', 'A', 'Z', 'P', 0}, Price: 187.3},
	}
	for _, s := range structs {
		if err := binary.Write(buf, binary.LittleEndian, s); err != nil {
			t.Fatal(err)
		}
	}

	out := make(chan model.Quote, 10)
	r := NewReader(slog.New(slog.NewTextHandler(io.Discard, nil)))
	r.readFrom(context.Background(), buf, out) // вернёт ошибку EOF — это нормально в тесте
	close(out)

	var got []model.Quote
	for q := range out {
		got = append(got, q)
	}

	if len(got) != 2 {
		t.Fatalf("expected 2 quotes, got %d", len(got))
	}
	if got[0].Ticker != "SBER" {
		t.Errorf("ticker[0]: got %q, want %q", got[0].Ticker, "SBER")
	}
	if got[1].Ticker != "GAZP" {
		t.Errorf("ticker[1]: got %q, want %q", got[1].Ticker, "GAZP")
	}
	// Цена из float32 → float64; допускаем небольшую погрешность
	if got[0].Price < 252.4 || got[0].Price > 252.6 {
		t.Errorf("price[0]: got %f, want ~252.5", got[0].Price)
	}
	if got[0].TimestampMs == 0 {
		t.Error("TimestampMs should not be zero")
	}
}

// TestReader_Run проверяет открытие файла и чтение через публичный метод Run.
func TestReader_Run(t *testing.T) {
	f, err := os.CreateTemp("", "driver-test-*")
	if err != nil {
		t.Fatal(err)
	}
	defer os.Remove(f.Name())

	binary.Write(f, binary.LittleEndian, rawQuote{Ticker: [8]byte{'R', 'U', 'N', 0}, Price: 99.9})
	f.Close()

	out := make(chan model.Quote, 10)
	r := NewReader(slog.New(slog.NewTextHandler(io.Discard, nil)))

	// Run вернёт ошибку EOF — файл конечный, это ожидаемо в тесте.
	r.Run(context.Background(), f.Name(), out) //nolint:errcheck
	close(out)

	var got []model.Quote
	for q := range out {
		got = append(got, q)
	}
	if len(got) != 1 || got[0].Ticker != "RUN" {
		t.Fatalf("unexpected result: %+v", got)
	}
}

// TestReader_StopsOnContextCancel проверяет, что readFrom возвращает nil (не ошибку)
// при отмене контекста — graceful shutdown.
func TestReader_StopsOnContextCancel(t *testing.T) {
	// io.Pipe — синхронный канал: Read блокируется до Write или Close.
	pr, pw := io.Pipe()

	out := make(chan model.Quote, 10)
	r := NewReader(slog.New(slog.NewTextHandler(io.Discard, nil)))

	ctx, cancel := context.WithCancel(context.Background())

	done := make(chan error, 1)
	go func() {
		done <- r.readFrom(ctx, pr, out)
	}()

	cancel()   // сначала отменяем контекст
	pw.Close() // затем разблокируем зависший ReadFull

	if err := <-done; err != nil {
		t.Errorf("expected nil on ctx cancel, got: %v", err)
	}
}

func TestReader_DropsWhenChannelFull(t *testing.T) {
	buf := &bytes.Buffer{}
	for i := 0; i < 5; i++ {
		binary.Write(buf, binary.LittleEndian, rawQuote{
			Ticker: [8]byte{'S', 0},
			Price:  float32(i),
		})
	}

	// Канал вмещает только 2 — остальные должны быть задропаны.
	out := make(chan model.Quote, 2)
	r := NewReader(slog.New(slog.NewTextHandler(io.Discard, nil)))
	r.readFrom(context.Background(), buf, out)
	close(out)

	var count int
	for range out {
		count++
	}
	if count != 2 {
		t.Errorf("expected 2 (channel capacity), got %d", count)
	}
}
