package batcher

import (
	"context"
	"crypto/tls"
	"fmt"
	"log/slog"
	"regexp"
	"strings"
	"sync"
	"time"

	clickhouse "github.com/ClickHouse/clickhouse-go/v2"
	chdriver "github.com/ClickHouse/clickhouse-go/v2/lib/driver"
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

type Config struct {
	Addr          string
	Database      string
	User          string
	Password      string
	Table         string
	BatchSize     int
	FlushInterval time.Duration
	InsertTimeout time.Duration
	RetryInitial  time.Duration
	RetryMax      time.Duration
	DialTimeout   time.Duration
	TLS           bool
}

type quoteInserter interface {
	Insert(ctx context.Context, quotes []model.Quote) error
	Close() error
}

// ClickHouseBatcher буферизует котировки и пишет их в ClickHouse крупными insert-батчами.
type ClickHouseBatcher struct {
	insert        quoteInserter
	log           *slog.Logger
	batchSize     int
	flushInterval time.Duration
	insertTimeout time.Duration
	retryInitial  time.Duration
	retryMax      time.Duration

	mu     sync.Mutex
	buffer []model.Quote

	ticker *time.Ticker
	done   chan struct{}
	once   sync.Once
	wg     sync.WaitGroup
}

func NewClickHouseBatcher(ctx context.Context, cfg Config, log *slog.Logger) (*ClickHouseBatcher, error) {
	insert, err := newClickHouseInserter(cfg)
	if err != nil {
		return nil, err
	}
	return newClickHouseBatcherWithInserter(ctx, cfg, log, insert), nil
}

func newClickHouseBatcherWithInserter(ctx context.Context, cfg Config, log *slog.Logger, insert quoteInserter) *ClickHouseBatcher {
	b := &ClickHouseBatcher{
		insert:        insert,
		log:           log,
		batchSize:     cfg.BatchSize,
		flushInterval: cfg.FlushInterval,
		insertTimeout: cfg.InsertTimeout,
		retryInitial:  cfg.RetryInitial,
		retryMax:      cfg.RetryMax,
		buffer:        make([]model.Quote, 0, cfg.BatchSize),
		ticker:        time.NewTicker(cfg.FlushInterval),
		done:          make(chan struct{}),
	}

	b.wg.Add(1)
	go b.flushLoop(ctx)

	return b
}

func (b *ClickHouseBatcher) Add(ctx context.Context, q model.Quote) error {
	if err := ctx.Err(); err != nil {
		return err
	}

	var flush []model.Quote
	b.mu.Lock()
	b.buffer = append(b.buffer, q)
	if len(b.buffer) >= b.batchSize {
		flush = b.takeLocked()
	}
	b.mu.Unlock()

	if len(flush) == 0 {
		return nil
	}
	return b.insertWithRetry(ctx, flush)
}

func (b *ClickHouseBatcher) Close(ctx context.Context) error {
	var closeErr error
	b.once.Do(func() {
		close(b.done)
		b.ticker.Stop()
		b.wg.Wait()

		if err := b.Flush(ctx); err != nil {
			closeErr = err
		}
		if err := b.insert.Close(); err != nil && closeErr == nil {
			closeErr = err
		}
	})
	return closeErr
}

func (b *ClickHouseBatcher) Flush(ctx context.Context) error {
	b.mu.Lock()
	flush := b.takeLocked()
	b.mu.Unlock()

	if len(flush) == 0 {
		return nil
	}
	return b.insertWithRetry(ctx, flush)
}

func (b *ClickHouseBatcher) flushLoop(ctx context.Context) {
	defer b.wg.Done()
	for {
		select {
		case <-b.ticker.C:
			if err := b.Flush(ctx); err != nil {
				b.log.Error("clickhouse batch flush failed", "err", err)
			}
		case <-ctx.Done():
			return
		case <-b.done:
			return
		}
	}
}

func (b *ClickHouseBatcher) takeLocked() []model.Quote {
	if len(b.buffer) == 0 {
		return nil
	}
	flush := make([]model.Quote, len(b.buffer))
	copy(flush, b.buffer)
	b.buffer = b.buffer[:0]
	return flush
}

func (b *ClickHouseBatcher) insertWithRetry(ctx context.Context, quotes []model.Quote) error {
	delay := b.retryInitial
	for attempt := 1; ; attempt++ {
		insertCtx, cancel := context.WithTimeout(ctx, b.insertTimeout)
		err := b.insert.Insert(insertCtx, quotes)
		cancel()
		if err == nil {
			b.log.Debug("clickhouse batch inserted", "size", len(quotes), "attempt", attempt)
			return nil
		}

		if ctx.Err() != nil {
			return fmt.Errorf("clickhouse insert canceled after attempt %d: %w", attempt, ctx.Err())
		}

		b.log.Warn("clickhouse batch insert failed, retrying",
			"size", len(quotes),
			"attempt", attempt,
			"backoff", delay.String(),
			"err", err,
		)

		select {
		case <-time.After(delay):
			delay *= 2
			if delay > b.retryMax {
				delay = b.retryMax
			}
		case <-ctx.Done():
			return fmt.Errorf("clickhouse insert canceled during backoff: %w", ctx.Err())
		}
	}
}

type clickHouseInserter struct {
	conn  chdriver.Conn
	table string
}

var openClickHouse = clickhouse.Open

func newClickHouseInserter(cfg Config) (*clickHouseInserter, error) {
	if err := validateTableName(cfg.Table); err != nil {
		return nil, err
	}

	options := &clickhouse.Options{
		Addr: []string{cfg.Addr},
		Auth: clickhouse.Auth{
			Database: cfg.Database,
			Username: cfg.User,
			Password: cfg.Password,
		},
		DialTimeout: cfg.DialTimeout,
	}
	if cfg.TLS {
		options.TLS = &tls.Config{MinVersion: tls.VersionTLS12}
	}

	conn, err := openClickHouse(options)
	if err != nil {
		return nil, fmt.Errorf("clickhouse open: %w", err)
	}
	pingCtx, cancel := context.WithTimeout(context.Background(), cfg.DialTimeout)
	defer cancel()
	if err := conn.Ping(pingCtx); err != nil {
		_ = conn.Close()
		return nil, fmt.Errorf("clickhouse ping: %w", err)
	}

	return &clickHouseInserter{conn: conn, table: cfg.Table}, nil
}

func (i *clickHouseInserter) Insert(ctx context.Context, quotes []model.Quote) error {
	if len(quotes) == 0 {
		return nil
	}

	batch, err := i.conn.PrepareBatch(ctx,
		fmt.Sprintf("INSERT INTO %s (symbol, event_time, price, ingested_at)", i.table))
	if err != nil {
		return fmt.Errorf("clickhouse prepare batch: %w", err)
	}

	now := time.Now().UTC()
	appended := 0
	for _, q := range quotes {
		if q.Ticker == "" {
			continue
		}
		eventTime := time.UnixMilli(q.TimestampMs).UTC()
		if err := batch.Append(q.Ticker, eventTime, q.Price, now); err != nil {
			return fmt.Errorf("clickhouse append quote: %w", err)
		}
		appended++
	}

	if appended == 0 {
		if err := batch.Abort(); err != nil {
			return fmt.Errorf("clickhouse abort empty batch: %w", err)
		}
		return nil
	}

	if err := batch.Send(); err != nil {
		return fmt.Errorf("clickhouse send batch: %w", err)
	}
	return nil
}

func (i *clickHouseInserter) Close() error {
	return i.conn.Close()
}

var tableNameRE = regexp.MustCompile(`^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)?$`)

func validateTableName(table string) error {
	if strings.TrimSpace(table) != table || !tableNameRE.MatchString(table) {
		return fmt.Errorf("invalid CLICKHOUSE_TABLE %q", table)
	}
	return nil
}
