//go:build integration

package batcher

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"testing"
	"time"

	clickhouse "github.com/ClickHouse/clickhouse-go/v2"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/wait"
	"github.com/tradingexchange/go-collector/internal/model"
)

func TestClickHouseBatcherIntegration(t *testing.T) {
	testcontainers.SkipIfProviderIsNotHealthy(t)

	ctx := context.Background()

	req := testcontainers.ContainerRequest{
		Image:        envOr("TEST_CLICKHOUSE_IMAGE", "clickhouse/clickhouse-server:25.3-alpine"),
		ExposedPorts: []string{"9000/tcp"},
		Env: map[string]string{
			"CLICKHOUSE_DB":                        "trading",
			"CLICKHOUSE_USER":                      "trading_app",
			"CLICKHOUSE_PASSWORD":                  "integration-password",
			"CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT": "1",
		},
		WaitingFor: wait.ForListeningPort("9000/tcp").WithStartupTimeout(2 * time.Minute),
	}

	container, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
		ContainerRequest: req,
		Started:          true,
	})
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		_ = container.Terminate(context.Background())
	})

	host, err := container.Host(ctx)
	if err != nil {
		t.Fatal(err)
	}
	port, err := container.MappedPort(ctx, "9000/tcp")
	if err != nil {
		t.Fatal(err)
	}
	addr := fmt.Sprintf("%s:%s", host, port.Port())

	conn, err := clickhouse.Open(&clickhouse.Options{
		Addr: []string{addr},
		Auth: clickhouse.Auth{
			Database: "trading",
			Username: "trading_app",
			Password: "integration-password",
		},
		DialTimeout: 5 * time.Second,
	})
	if err != nil {
		t.Fatal(err)
	}
	defer conn.Close() //nolint:errcheck

	for _, stmt := range []string{
		`CREATE TABLE IF NOT EXISTS trading.quotes_raw
		(
			symbol LowCardinality(String),
			event_time DateTime64(3, 'UTC'),
			price Float64,
			ingested_at DateTime64(3, 'UTC') DEFAULT now64(3)
		)
		ENGINE = MergeTree
		PARTITION BY toYYYYMM(event_time)
		ORDER BY (symbol, event_time)`,
	} {
		if err := conn.Exec(ctx, stmt); err != nil {
			t.Fatal(err)
		}
	}

	b, err := NewClickHouseBatcher(ctx, Config{
		Addr:          addr,
		Database:      "trading",
		User:          "trading_app",
		Password:      "integration-password",
		Table:         "trading.quotes_raw",
		BatchSize:     2,
		FlushInterval: time.Second,
		InsertTimeout: 5 * time.Second,
		RetryInitial:  10 * time.Millisecond,
		RetryMax:      50 * time.Millisecond,
		DialTimeout:   5 * time.Second,
	}, slog.New(slog.NewTextHandler(os.Stderr, nil)))
	if err != nil {
		t.Fatal(err)
	}

	now := time.Now().UTC().UnixMilli()
	if err := b.Add(ctx, model.Quote{Ticker: "SBER", Price: 250.5, TimestampMs: now}); err != nil {
		t.Fatal(err)
	}
	if err := b.Add(ctx, model.Quote{Ticker: "SBER", Price: 251.0, TimestampMs: now + 1}); err != nil {
		t.Fatal(err)
	}
	if err := b.Close(ctx); err != nil {
		t.Fatal(err)
	}

	var count uint64
	if err := conn.QueryRow(ctx, "SELECT count() FROM trading.quotes_raw WHERE symbol = 'SBER'").Scan(&count); err != nil {
		t.Fatal(err)
	}
	if count != 2 {
		t.Fatalf("expected 2 rows, got %d", count)
	}
}

func envOr(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}
