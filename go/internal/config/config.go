package config

import (
	"fmt"
	"os"
	"strconv"
	"time"
)

type Config struct {
	DriverDevicePath string
	LogLevel         string
	ClickHouse       ClickHouseConfig
}

type ClickHouseConfig struct {
	Enabled       bool
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
}

func Load() (*Config, error) {
	devicePath := os.Getenv("DRIVER_DEVICE_PATH")
	if devicePath == "" {
		return nil, fmt.Errorf("DRIVER_DEVICE_PATH is required")
	}
	clickHouse, err := loadClickHouse()
	if err != nil {
		return nil, err
	}
	return &Config{
		DriverDevicePath: devicePath,
		LogLevel:         envOr("LOG_LEVEL", "info"),
		ClickHouse:       clickHouse,
	}, nil
}

func envOr(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

func loadClickHouse() (ClickHouseConfig, error) {
	enabled, err := envBool("CLICKHOUSE_ENABLED", false)
	if err != nil {
		return ClickHouseConfig{}, err
	}

	cfg := ClickHouseConfig{
		Enabled:       enabled,
		Addr:          envOr("CLICKHOUSE_ADDR", "localhost:9000"),
		Database:      envOr("CLICKHOUSE_DATABASE", "trading"),
		User:          envOr("CLICKHOUSE_USER", "trading_app"),
		Password:      os.Getenv("CLICKHOUSE_PASSWORD"),
		Table:         envOr("CLICKHOUSE_TABLE", "trading.quotes_raw"),
		BatchSize:     envInt("CLICKHOUSE_BATCH_SIZE", 10000),
		FlushInterval: envDurationMs("CLICKHOUSE_FLUSH_INTERVAL_MS", 200),
		InsertTimeout: envDurationMs("CLICKHOUSE_INSERT_TIMEOUT_MS", 5000),
		RetryInitial:  envDurationMs("CLICKHOUSE_RETRY_INITIAL_MS", 100),
		RetryMax:      envDurationMs("CLICKHOUSE_RETRY_MAX_MS", 2000),
		DialTimeout:   envDurationMs("CLICKHOUSE_DIAL_TIMEOUT_MS", 5000),
	}

	if cfg.BatchSize <= 0 {
		return ClickHouseConfig{}, fmt.Errorf("CLICKHOUSE_BATCH_SIZE must be positive")
	}
	if cfg.FlushInterval <= 0 {
		return ClickHouseConfig{}, fmt.Errorf("CLICKHOUSE_FLUSH_INTERVAL_MS must be positive")
	}
	if cfg.InsertTimeout <= 0 {
		return ClickHouseConfig{}, fmt.Errorf("CLICKHOUSE_INSERT_TIMEOUT_MS must be positive")
	}
	if cfg.RetryInitial <= 0 || cfg.RetryMax <= 0 || cfg.RetryInitial > cfg.RetryMax {
		return ClickHouseConfig{}, fmt.Errorf("CLICKHOUSE_RETRY_INITIAL_MS must be <= CLICKHOUSE_RETRY_MAX_MS and both positive")
	}
	if cfg.Enabled && cfg.Password == "" {
		return ClickHouseConfig{}, fmt.Errorf("CLICKHOUSE_PASSWORD is required when CLICKHOUSE_ENABLED=true")
	}

	return cfg, nil
}

func envBool(key string, def bool) (bool, error) {
	v := os.Getenv(key)
	if v == "" {
		return def, nil
	}
	parsed, err := strconv.ParseBool(v)
	if err != nil {
		return false, fmt.Errorf("%s must be boolean: %w", key, err)
	}
	return parsed, nil
}

func envInt(key string, def int) int {
	v := os.Getenv(key)
	if v == "" {
		return def
	}
	parsed, err := strconv.Atoi(v)
	if err != nil {
		return def
	}
	return parsed
}

func envDurationMs(key string, def int) time.Duration {
	return time.Duration(envInt(key, def)) * time.Millisecond
}
