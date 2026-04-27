package config

import (
	"strings"
	"testing"
	"time"
)

func TestLoadRequiresDriverDevicePath(t *testing.T) {
	t.Setenv("DRIVER_DEVICE_PATH", "")

	_, err := Load()
	if err == nil || !strings.Contains(err.Error(), "DRIVER_DEVICE_PATH") {
		t.Fatalf("expected DRIVER_DEVICE_PATH error, got %v", err)
	}
}

func TestLoadDefaultsClickHouseDisabled(t *testing.T) {
	t.Setenv("DRIVER_DEVICE_PATH", "/dev/quotes")

	cfg, err := Load()
	if err != nil {
		t.Fatal(err)
	}

	if cfg.ClickHouse.Enabled {
		t.Fatal("clickhouse should be disabled by default")
	}
	if cfg.ClickHouse.BatchSize != 10000 {
		t.Fatalf("unexpected batch size: %d", cfg.ClickHouse.BatchSize)
	}
	if cfg.ClickHouse.FlushInterval != 200*time.Millisecond {
		t.Fatalf("unexpected flush interval: %s", cfg.ClickHouse.FlushInterval)
	}
}

func TestLoadClickHouseEnabledRequiresPassword(t *testing.T) {
	t.Setenv("DRIVER_DEVICE_PATH", "/dev/quotes")
	t.Setenv("CLICKHOUSE_ENABLED", "true")
	t.Setenv("CLICKHOUSE_PASSWORD", "")

	_, err := Load()
	if err == nil || !strings.Contains(err.Error(), "CLICKHOUSE_PASSWORD") {
		t.Fatalf("expected CLICKHOUSE_PASSWORD error, got %v", err)
	}
}

func TestLoadClickHouseOverrides(t *testing.T) {
	t.Setenv("DRIVER_DEVICE_PATH", "/dev/quotes")
	t.Setenv("CLICKHOUSE_ENABLED", "true")
	t.Setenv("CLICKHOUSE_PASSWORD", "secret")
	t.Setenv("CLICKHOUSE_ADDR", "clickhouse:9000")
	t.Setenv("CLICKHOUSE_BATCH_SIZE", "500")
	t.Setenv("CLICKHOUSE_FLUSH_INTERVAL_MS", "50")

	cfg, err := Load()
	if err != nil {
		t.Fatal(err)
	}

	if !cfg.ClickHouse.Enabled {
		t.Fatal("clickhouse should be enabled")
	}
	if cfg.ClickHouse.Addr != "clickhouse:9000" {
		t.Fatalf("unexpected addr: %s", cfg.ClickHouse.Addr)
	}
	if cfg.ClickHouse.BatchSize != 500 {
		t.Fatalf("unexpected batch size: %d", cfg.ClickHouse.BatchSize)
	}
	if cfg.ClickHouse.FlushInterval != 50*time.Millisecond {
		t.Fatalf("unexpected flush interval: %s", cfg.ClickHouse.FlushInterval)
	}
}
