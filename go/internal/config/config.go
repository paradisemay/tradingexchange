package config

import (
	"fmt"
	"os"
)

type Config struct {
	DriverDevicePath string
	LogLevel         string
}

func Load() (*Config, error) {
	devicePath := os.Getenv("DRIVER_DEVICE_PATH")
	if devicePath == "" {
		return nil, fmt.Errorf("DRIVER_DEVICE_PATH is required")
	}
	return &Config{
		DriverDevicePath: devicePath,
		LogLevel:         envOr("LOG_LEVEL", "info"),
	}, nil
}

func envOr(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}
