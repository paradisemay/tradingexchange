# Интеграционные блокеры и зависимости от других команд

> Документ фиксирует всё, что нельзя завершить без результатов других модулей.  
> Обновлять по мере закрытия пунктов.

---

## 🔴 Критично — мешает работе котировок прямо сейчас

### Go-команда (girneos / ttemmaaa / amsterdam121): proto-схема `quotes.v1`

**Файл у нас:** `server/src/main/kotlin/com/example/quotes/QuoteTick.kt`

```kotlin
@Serializable
data class QuoteTick(
    @ProtoNumber(1) val ticker: String = "",
    @ProtoNumber(2) val price: Float = 0f,
    @ProtoNumber(3) val timestampMs: Long = 0L,
    @ProtoNumber(4) val currency: String = "RUB",
)
```

**Проблема:** `@ProtoNumber` — предположение. Если Go-команда использует другой порядок или другие номера полей — `ProtoBuf.decodeFromByteArray` вернёт мусор (поля перепутаются) или бросит исключение (тип не совпадёт). В таком случае Ktor будет молча пропускать все сообщения.

**Что нужно от Go-команды:**
- Опубликовать `.proto`-файл `quotes.v1.proto` в репозиторий
- Конкретно нужны: имена полей, `proto field number` каждого, тип `price` (float32 vs double vs string)

**Что сделать после получения схемы:**
1. Сверить `@ProtoNumber(N)` в `QuoteTick.kt` с реальными номерами полей
2. Если тип `price` — `double` или `string`, поменять тип поля
3. Убрать TODO-комментарий в файле
4. Прогнать интеграционный тест: `XADD stream:quotes:v1 ... payload <real-proto-bytes>` → Ktor получает WebSocket-тик

**Временный workaround для тестирования до получения схемы:**
```bash
# Go-команда или мы сами можем опубликовать в стрим произвольные байты — Ktor просто пропустит их (log.debug)
# Для smoke-теста можно сериализовать QuoteTick локально и вручную пушить в стрим
```

---

## 🟡 Важно — нужно для полной наблюдаемости

### Telemetry-команда (stella_stt / Bandz): OTel Collector

**Что у нас настроено:** OTel SDK подключён, HTTP-инструментация активна. Экспорт **отключён по умолчанию** (`OTEL_TRACES_EXPORTER=none`) чтобы не шумел в логи при отсутствии коллектора.

**Что нужно от команды:**
- Поднять `otel-collector` в общем docker-compose (или задокументировать порт/endpoint в их модуле)
- Согласовать сетевое имя контейнера (мы используем `otel-collector` как hostname)

**Что сделать после:**
1. В `docker-compose.yml` выставить:
   ```yaml
   OTEL_TRACES_EXPORTER: otlp
   OTEL_METRICS_EXPORTER: otlp
   OTEL_LOGS_EXPORTER: otlp
   OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4318
   ```
2. Добавить `broker-net` в список сетей контейнера `otel-collector` ИЛИ оба сервиса перевести в общую сеть
3. Проверить что в Tempo появляются трейсы HTTP-запросов Ktor

**Ещё не реализовано у нас (нужно сделать самим, не зависит от других):**
- Ручные spans вокруг DB-транзакции BUY/SELL (длительность операции)
- Метрики: `ws_active_connections`, `redis_reconnect_count`, `db_pool_usage`

---

## 🟡 Важно — нужно для графиков в мобильном клиенте

### ClickHouse-команда (amsterdam121 / weebat): исторические котировки

По SRS, Ktor должен отдавать мобильному клиенту:
- Линейный график: `GET /api/v1/charts/{ticker}/line?from=&to=` → `[(timestamp, price)]`
- Свечи: `GET /api/v1/charts/{ticker}/candles?interval=1m&from=&to=` → `[(time, open, high, low, close)]`
- Summary: min/max/first/last за диапазон

**Эти эндпоинты у нас НЕ реализованы** — ждём от ClickHouse-команды:
- DDL таблиц `quotes_raw` и `quotes_ohlc_1m`
- Конфигурацию подключения (host, port, database)
- Примеры SQL-запросов для line/candles/summary

**Что добавить с нашей стороны после:**
1. Зависимость `clickhouse-java` в `build.gradle.kts`
2. `ClickHouseRepository` с тремя методами (line, candles, summary)
3. Три новых GET-эндпоинта
4. Обновить `swagger.yaml`

---

## 🟢 Ожидаем, но не блокирует нашу работу

### Redis-команда (tteemma): сетевая интеграция

Коллега поднял свой Redis в отдельном `docker-compose` с сетью `redis-net`.  
Наш Ktor использует собственный Redis (`broker-net`).

**При финальной интеграции** нужно решить один из вариантов:
- А) Использовать Redis-модуль коллеги — подключить наш `broker-ktor` к `redis-net`
- Б) Оставить наш Redis, Go-команда пишет в него напрямую
- В) Единый `docker-compose` на уровне репозитория

Сейчас у нас работает вариант Б (наш Redis, независимый стек).

---

## Сводная таблица

| Команда | Блокер | Критичность | Файл у нас |
|---|---|---|---|
| **Go** (girneos) | `quotes.v1.proto` — номера полей Protobuf | 🔴 Критично | `quotes/QuoteTick.kt` |
| **Telemetry** (stella_stt) | `otel-collector` endpoint и сеть | 🟡 Важно | `docker-compose.yml`, `plugins/OpenTelemetry.kt` |
| **ClickHouse** (amsterdam121) | DDL схема + SQL-запросы для графиков | 🟡 Важно | Эндпоинты не реализованы |
| **Redis** (tteemma) | Финальная сетевая топология | 🟢 Некритично | `docker-compose.yml` |

---

## Контрольные вопросы при ревью интеграции

- [ ] Go опубликовал `quotes.v1.proto` → сверены `@ProtoNumber` в `QuoteTick.kt`
- [ ] Ktor получает реальные тики из `stream:quotes:v1` → WebSocket-клиент видит обновления цен
- [ ] `OTEL_TRACES_EXPORTER=otlp` включён → в Grafana/Tempo видны трейсы
- [ ] ClickHouse-эндпоинты реализованы и покрыты тестами
- [ ] Все модули в одной Docker-сети → `docker compose up` поднимает всё одной командой
