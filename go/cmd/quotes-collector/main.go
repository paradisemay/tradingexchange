package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"sync"
	"syscall"

	"github.com/tradingexchange/go-collector/internal/batcher"
	"github.com/tradingexchange/go-collector/internal/config"
	"github.com/tradingexchange/go-collector/internal/driver"
	"github.com/tradingexchange/go-collector/internal/model"
	"github.com/tradingexchange/go-collector/internal/pipeline"
	"github.com/tradingexchange/go-collector/internal/publisher"
)

func main() {
	log := slog.New(slog.NewJSONHandler(os.Stdout, nil))

	cfg, err := config.Load()
	if err != nil {
		log.Error("failed to load config", "err", err)
		os.Exit(1)
	}

	// signal.NotifyContext — аналог Runtime.addShutdownHook() в Java.
	// При получении SIGINT/SIGTERM ctx отменяется и все горутины получают сигнал остановки.
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	// Канал — аналог BlockingQueue в Java. Буфер 1000 защищает от кратковременных
	// задержек в downstream, не блокируя чтение из устройства.
	quotes := make(chan model.Quote, 1000)

	pub := publisher.NewLogPublisher(log)
	bat := batcher.NewLogBatcher(log)

	var wg sync.WaitGroup // аналог CountDownLatch

	// Горутина 1: читает котировки из C-драйвера и кладёт в канал.
	wg.Add(1)
	go func() {
		defer wg.Done()
		defer close(quotes) // сигнализирует pipeline о завершении чтения

		r := driver.NewReader(log)
		if err := r.Run(ctx, cfg.DriverDevicePath, quotes); err != nil {
			log.Error("driver error", "err", err)
		}
	}()

	// Горутина 2: читает из канала и передаёт каждую котировку в pub и bat.
	wg.Add(1)
	go func() {
		defer wg.Done()
		pipeline.Run(ctx, quotes, pub, bat, log)
	}()

	log.Info("quotes-collector started", "device", cfg.DriverDevicePath)
	wg.Wait()

	// Graceful shutdown: сбрасываем остаток буфера батчера.
	if err := bat.Close(context.Background()); err != nil {
		log.Error("batcher close error", "err", err)
	}
	pub.Close()

	log.Info("shutdown complete")
}
