package model

// Quote — доменная структура одной котировки.
// Создаётся драйвером после чтения из C-устройства и обогащается временной меткой.
type Quote struct {
	Ticker      string  // символ инструмента, например "SBER"
	Price       float64 // цена (float32 из C-драйвера расширяется до float64)
	TimestampMs int64   // Unix timestamp в миллисекундах (UTC), момент чтения
}
