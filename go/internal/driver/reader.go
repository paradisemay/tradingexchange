package driver

import (
	"bytes"
	"context"
	"encoding/binary"
	"fmt"
	"io"
	"log/slog"
	"os"
	"time"

	"github.com/tradingexchange/go-collector/internal/model"
)

// rawQuote отражает C-структуру из драйвера:
//
//	struct mock { char ticker[8]; float price; }
//
// Размер: 8 + 4 = 12 байт, выравнивание совпадает.
type rawQuote struct {
	Ticker [8]byte
	Price  float32 // C float = IEEE 754 single precision = Go float32
}

const rawQuoteSize = 12

// Reader читает бинарный поток котировок из файла устройства C-драйвера.
type Reader struct {
	log *slog.Logger
}

func NewReader(log *slog.Logger) *Reader {
	return &Reader{log: log}
}

// Run открывает файл устройства и запускает цикл чтения до отмены ctx.
// При отмене ctx файл закрывается, что разблокирует зависший io.ReadFull.
// Аналог Java: Callable, запущенный в ExecutorService — блокирует поток до сигнала остановки.
func (r *Reader) Run(ctx context.Context, devicePath string, out chan<- model.Quote) error {
	f, err := os.Open(devicePath)
	if err != nil {
		return fmt.Errorf("driver: open %q: %w", devicePath, err)
	}

	// Закрываем файл при отмене контекста, чтобы разблокировать ReadFull ниже.
	// defer f.Close() не нужен: закрытие через горутину выполнится раньше.
	go func() {
		<-ctx.Done()
		f.Close()
	}()

	return r.readFrom(ctx, f, out)
}

// readFrom читает rawQuote-структуры из rd и отправляет model.Quote в out.
// Вынесено отдельно для тестируемости: в тестах передаём bytes.Buffer вместо файла.
func (r *Reader) readFrom(ctx context.Context, rd io.Reader, out chan<- model.Quote) error {
	buf := make([]byte, rawQuoteSize)

	for {
		_, err := io.ReadFull(rd, buf)
		if err != nil {
			// Если контекст отменён — это нормальное завершение, не ошибка.
			if ctx.Err() != nil {
				r.log.Info("driver: stopped")
				return nil
			}
			return fmt.Errorf("driver: read: %w", err)
		}

		var raw rawQuote
		// bytes.NewReader не аллоцирует — переиспользует уже прочитанный buf.
		if err := binary.Read(bytes.NewReader(buf), binary.LittleEndian, &raw); err != nil {
			r.log.Warn("driver: malformed struct, skipping", "err", err)
			continue
		}

		q := model.Quote{
			Ticker:      cStringToGo(raw.Ticker[:]),
			Price:       float64(raw.Price),
			TimestampMs: time.Now().UnixMilli(),
		}

		// Неблокирующая отправка: если канал полон — логируем и дропаем тик,
		// но не тормозим чтение из устройства (буфер драйвера может переполниться).
		select {
		case out <- q:
		case <-ctx.Done():
			return nil
		default:
			r.log.Warn("driver: output channel full, dropping quote", "ticker", q.Ticker)
		}
	}
}

// cStringToGo конвертирует null-terminated C-строку в Go string.
// Пример: [83 66 69 82 0 0 0 0] → "SBER"
func cStringToGo(b []byte) string {
	if i := bytes.IndexByte(b, 0); i >= 0 {
		b = b[:i]
	}
	return string(b)
}
