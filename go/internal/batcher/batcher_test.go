package batcher

import (
	"context"
	"errors"
	"io"
	"log/slog"
	"sync"
	"testing"
	"time"

	clickhouse "github.com/ClickHouse/clickhouse-go/v2"
	"github.com/ClickHouse/clickhouse-go/v2/lib/column"
	chdriver "github.com/ClickHouse/clickhouse-go/v2/lib/driver"
	"github.com/tradingexchange/go-collector/internal/model"
)

func TestClickHouseBatcherFlushesOnBatchSize(t *testing.T) {
	writer := &fakeInserter{}
	b := newTestBatcher(writer, Config{
		BatchSize:     3,
		FlushInterval: time.Hour,
	})
	defer b.Close(context.Background()) //nolint:errcheck

	for i := 0; i < 3; i++ {
		if err := b.Add(context.Background(), model.Quote{Ticker: "SBER", Price: float64(i), TimestampMs: time.Now().UnixMilli()}); err != nil {
			t.Fatal(err)
		}
	}

	batches := writer.batches()
	if len(batches) != 1 {
		t.Fatalf("expected 1 batch, got %d", len(batches))
	}
	if len(batches[0]) != 3 {
		t.Fatalf("expected 3 quotes, got %d", len(batches[0]))
	}
}

func TestClickHouseBatcherFlushesOnClose(t *testing.T) {
	writer := &fakeInserter{}
	b := newTestBatcher(writer, Config{
		BatchSize:     10,
		FlushInterval: time.Hour,
	})

	if err := b.Add(context.Background(), model.Quote{Ticker: "GAZP", Price: 101, TimestampMs: time.Now().UnixMilli()}); err != nil {
		t.Fatal(err)
	}
	if err := b.Close(context.Background()); err != nil {
		t.Fatal(err)
	}

	batches := writer.batches()
	if len(batches) != 1 || len(batches[0]) != 1 {
		t.Fatalf("unexpected batches: %+v", batches)
	}
	if !writer.closed() {
		t.Fatal("writer should be closed")
	}
}

func TestClickHouseBatcherTimedFlush(t *testing.T) {
	writer := &fakeInserter{}
	b := newTestBatcher(writer, Config{
		BatchSize:     10,
		FlushInterval: 10 * time.Millisecond,
	})
	defer b.Close(context.Background()) //nolint:errcheck

	if err := b.Add(context.Background(), model.Quote{Ticker: "YNDX", Price: 202, TimestampMs: time.Now().UnixMilli()}); err != nil {
		t.Fatal(err)
	}

	deadline := time.After(300 * time.Millisecond)
	for {
		if len(writer.batches()) == 1 {
			return
		}
		select {
		case <-deadline:
			t.Fatal("timed flush did not happen")
		default:
			time.Sleep(5 * time.Millisecond)
		}
	}
}

func TestClickHouseBatcherRetriesTemporaryFailure(t *testing.T) {
	writer := &fakeInserter{failures: 2}
	b := newTestBatcher(writer, Config{
		BatchSize:     1,
		FlushInterval: time.Hour,
		RetryInitial:  time.Millisecond,
		RetryMax:      time.Millisecond,
	})
	defer b.Close(context.Background()) //nolint:errcheck

	if err := b.Add(context.Background(), model.Quote{Ticker: "LKOH", Price: 303, TimestampMs: time.Now().UnixMilli()}); err != nil {
		t.Fatal(err)
	}

	if got := writer.attempts(); got != 3 {
		t.Fatalf("expected 3 attempts, got %d", got)
	}
	if len(writer.batches()) != 1 {
		t.Fatalf("expected successful batch after retries")
	}
}

func TestValidateTableName(t *testing.T) {
	valid := []string{"quotes_raw", "trading.quotes_raw", "_db.table_1"}
	for _, table := range valid {
		if err := validateTableName(table); err != nil {
			t.Fatalf("expected %q to be valid: %v", table, err)
		}
	}

	invalid := []string{"", " trading.quotes_raw", "trading.quotes_raw ", "trading.quotes_raw;DROP", "trading.*"}
	for _, table := range invalid {
		if err := validateTableName(table); err == nil {
			t.Fatalf("expected %q to be invalid", table)
		}
	}
}

func TestNewClickHouseInserterOpensAndPings(t *testing.T) {
	oldOpen := openClickHouse
	defer func() { openClickHouse = oldOpen }()

	conn := &fakeClickHouseConn{batch: &fakeClickHouseBatch{}}
	var gotOptions *clickhouse.Options
	openClickHouse = func(options *clickhouse.Options) (chdriver.Conn, error) {
		gotOptions = options
		return conn, nil
	}

	inserter, err := newClickHouseInserter(Config{
		Addr:        "clickhouse:9000",
		Database:    "trading",
		User:        "trading_app",
		Password:    "secret",
		Table:       "trading.quotes_raw",
		DialTimeout: time.Second,
	})
	if err != nil {
		t.Fatal(err)
	}
	defer inserter.Close() //nolint:errcheck

	if gotOptions == nil {
		t.Fatal("open options were not captured")
	}
	if gotOptions.Addr[0] != "clickhouse:9000" || gotOptions.Auth.Database != "trading" || gotOptions.Auth.Username != "trading_app" {
		t.Fatalf("unexpected options: %+v", gotOptions)
	}
	if !conn.pinged {
		t.Fatal("connection should be pinged")
	}
}

func TestNewClickHouseInserterRejectsInvalidTableBeforeOpen(t *testing.T) {
	oldOpen := openClickHouse
	defer func() { openClickHouse = oldOpen }()

	called := false
	openClickHouse = func(options *clickhouse.Options) (chdriver.Conn, error) {
		called = true
		return &fakeClickHouseConn{}, nil
	}

	_, err := newClickHouseInserter(Config{Table: "bad;table", DialTimeout: time.Second})
	if err == nil {
		t.Fatal("expected invalid table error")
	}
	if called {
		t.Fatal("open should not be called for invalid table")
	}
}

func TestNewClickHouseInserterClosesOnPingError(t *testing.T) {
	oldOpen := openClickHouse
	defer func() { openClickHouse = oldOpen }()

	conn := &fakeClickHouseConn{pingErr: errors.New("ping failed")}
	openClickHouse = func(options *clickhouse.Options) (chdriver.Conn, error) {
		return conn, nil
	}

	_, err := newClickHouseInserter(Config{Table: "trading.quotes_raw", DialTimeout: time.Second})
	if err == nil {
		t.Fatal("expected ping error")
	}
	if !conn.closed {
		t.Fatal("connection should be closed on ping error")
	}
}

func TestNewClickHouseInserterReturnsOpenError(t *testing.T) {
	oldOpen := openClickHouse
	defer func() { openClickHouse = oldOpen }()

	openClickHouse = func(options *clickhouse.Options) (chdriver.Conn, error) {
		return nil, errors.New("open failed")
	}

	_, err := newClickHouseInserter(Config{Table: "trading.quotes_raw", DialTimeout: time.Second})
	if err == nil {
		t.Fatal("expected open error")
	}
}

func TestClickHouseInserterInsertBuildsBatch(t *testing.T) {
	batch := &fakeClickHouseBatch{}
	conn := &fakeClickHouseConn{batch: batch}
	inserter := &clickHouseInserter{conn: conn, table: "trading.quotes_raw"}

	ts := time.Date(2026, 4, 27, 10, 30, 0, 123000000, time.UTC).UnixMilli()
	err := inserter.Insert(context.Background(), []model.Quote{
		{Ticker: "SBER", Price: 250.5, TimestampMs: ts},
		{Ticker: "", Price: 1, TimestampMs: ts},
	})
	if err != nil {
		t.Fatal(err)
	}

	if conn.query != "INSERT INTO trading.quotes_raw (symbol, event_time, price, ingested_at)" {
		t.Fatalf("unexpected query: %s", conn.query)
	}
	if !batch.sent {
		t.Fatal("batch should be sent")
	}
	if len(batch.rows) != 1 {
		t.Fatalf("expected 1 appended row, got %d", len(batch.rows))
	}
	if batch.rows[0][0] != "SBER" || batch.rows[0][2] != 250.5 {
		t.Fatalf("unexpected row values: %+v", batch.rows[0])
	}
	eventTime, ok := batch.rows[0][1].(time.Time)
	if !ok || !eventTime.Equal(time.UnixMilli(ts).UTC()) {
		t.Fatalf("unexpected event_time: %+v", batch.rows[0][1])
	}
}

func TestClickHouseInserterAbortsEmptyBatch(t *testing.T) {
	batch := &fakeClickHouseBatch{}
	conn := &fakeClickHouseConn{batch: batch}
	inserter := &clickHouseInserter{conn: conn, table: "trading.quotes_raw"}

	err := inserter.Insert(context.Background(), []model.Quote{{Ticker: "", Price: 1}})
	if err != nil {
		t.Fatal(err)
	}
	if !batch.aborted {
		t.Fatal("empty batch should be aborted")
	}
	if batch.sent {
		t.Fatal("empty batch should not be sent")
	}
}

func newTestBatcher(writer *fakeInserter, cfg Config) *ClickHouseBatcher {
	if cfg.InsertTimeout == 0 {
		cfg.InsertTimeout = time.Second
	}
	if cfg.RetryInitial == 0 {
		cfg.RetryInitial = time.Millisecond
	}
	if cfg.RetryMax == 0 {
		cfg.RetryMax = time.Millisecond
	}
	return newClickHouseBatcherWithInserter(
		context.Background(),
		cfg,
		slog.New(slog.NewTextHandler(io.Discard, nil)),
		writer,
	)
}

type fakeInserter struct {
	mu       sync.Mutex
	sent     [][]model.Quote
	try      int
	failures int
	isClosed bool
}

func (f *fakeInserter) Insert(_ context.Context, quotes []model.Quote) error {
	f.mu.Lock()
	defer f.mu.Unlock()

	f.try++
	if f.try <= f.failures {
		return errors.New("temporary insert failure")
	}

	cp := make([]model.Quote, len(quotes))
	copy(cp, quotes)
	f.sent = append(f.sent, cp)
	return nil
}

func (f *fakeInserter) Close() error {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.isClosed = true
	return nil
}

func (f *fakeInserter) batches() [][]model.Quote {
	f.mu.Lock()
	defer f.mu.Unlock()

	cp := make([][]model.Quote, len(f.sent))
	copy(cp, f.sent)
	return cp
}

func (f *fakeInserter) attempts() int {
	f.mu.Lock()
	defer f.mu.Unlock()
	return f.try
}

func (f *fakeInserter) closed() bool {
	f.mu.Lock()
	defer f.mu.Unlock()
	return f.isClosed
}

type fakeClickHouseConn struct {
	chdriver.Conn
	batch   *fakeClickHouseBatch
	query   string
	closed  bool
	pinged  bool
	pingErr error
}

func (f *fakeClickHouseConn) Ping(context.Context) error {
	f.pinged = true
	return f.pingErr
}

func (f *fakeClickHouseConn) PrepareBatch(_ context.Context, query string, _ ...chdriver.PrepareBatchOption) (chdriver.Batch, error) {
	f.query = query
	return f.batch, nil
}

func (f *fakeClickHouseConn) Close() error {
	f.closed = true
	return nil
}

type fakeClickHouseBatch struct {
	rows    [][]any
	sent    bool
	aborted bool
}

func (f *fakeClickHouseBatch) Abort() error {
	f.aborted = true
	return nil
}

func (f *fakeClickHouseBatch) Append(v ...any) error {
	row := make([]any, len(v))
	copy(row, v)
	f.rows = append(f.rows, row)
	return nil
}

func (f *fakeClickHouseBatch) AppendStruct(any) error { return nil }
func (f *fakeClickHouseBatch) Column(int) chdriver.BatchColumn {
	return nil
}
func (f *fakeClickHouseBatch) Flush() error { return nil }
func (f *fakeClickHouseBatch) Send() error {
	f.sent = true
	return nil
}
func (f *fakeClickHouseBatch) IsSent() bool { return f.sent }
func (f *fakeClickHouseBatch) Rows() int    { return len(f.rows) }
func (f *fakeClickHouseBatch) Columns() []column.Interface {
	return nil
}

var _ chdriver.Conn = (*fakeClickHouseConn)(nil)
var _ chdriver.Batch = (*fakeClickHouseBatch)(nil)
