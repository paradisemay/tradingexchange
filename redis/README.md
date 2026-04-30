# Redis Module

Redis is the local high-speed broker and short-lived cache for the trading platform.

## Unified stack note

Для общего запуска всей архитектуры используйте корневой compose из корня репозитория.
Локальный `redis/docker-compose.yml` оставлен для изолированных проверок Redis.

This module is intentionally infrastructure-only. It does not encode, decode, or validate quote payloads. Redis stores quote messages as opaque bytes in Streams and stores temporary cache keys with explicit TTLs.

## Scope

This module owns only Redis infrastructure, Redis Streams broker capability, TTL cache keyspace documentation, telemetry, and verification commands.

Other services must adapt to this Redis contract. This module does not implement Go/Ktor compatibility layers, legacy JSON flows, Pub/Sub routing, or application message decoding.

## Services

- `redis`: Redis 7.x, AOF enabled, optional RDB snapshots, persistent `/data` volume.
- `redis-exporter`: Prometheus exporter on port `9121`.

## Configuration

Copy the example environment file:

```bash
cd /Users/temo4ka/ITMO/sem5/tradingexchange/redis
cp .env.example .env
```

Important settings:

| Variable | Default | Purpose |
| --- | --- | --- |
| `REDIS_PORT` | `6379` | Host port for local Redis access |
| `REDIS_PASSWORD` | empty | Optional Redis password |
| `REDIS_MAXMEMORY` | `512mb` | Redis memory limit |
| `REDIS_EXPORTER_PORT` | `9121` | Host port for metrics |
| `REDIS_STREAM` | `stream:quotes:v1` | Quote stream |
| `REDIS_CONSUMER_GROUP` | `cg:ktor:quotes` | Ktor consumer group |
| `REDIS_STREAM_MAXLEN` | `100000` | Approximate stream cap |

Secrets must come from environment values. Do not commit a real `.env`.

## Redis Config

`redis.conf` enables:

```conf
appendonly yes
appendfsync everysec
maxmemory-policy volatile-ttl
```

`docker-compose.yml` passes `--maxmemory ${REDIS_MAXMEMORY}` at startup because Redis config files do not expand environment variables.

## Quote Stream Contract

Primary stream:

```text
stream:quotes:v1
```

Ktor consumer group:

```text
cg:ktor:quotes
```

Consumer name pattern:

```text
ktor-instance-{n}
```

Publish command:

```bash
redis-cli XADD stream:quotes:v1 MAXLEN '~' 100000 '*' payload '<protobuf-bytes>'
```

The `payload` field is binary Protobuf bytes for `quotes.v1.QuoteTick`. Redis treats it as opaque bytes. Test commands may use `testdata`; production quote messages must not use JSON.

## Cache Keyspace

All temporary cache keys must have TTLs:

| Key | TTL | Purpose |
| --- | --- | --- |
| `cache:session:{sessionId}` | 30m | Session data |
| `cache:price:{symbol}` | 5s | Latest price cache |
| `cache:orderbook:{symbol}` | 1-2s | Hot order book snapshot |
| `cache:user:last-active:{userId}` | 10m | Last activity marker |

Redis is a cache layer, not a permanent database.

## Start Locally

```bash
cd /Users/temo4ka/ITMO/sem5/tradingexchange/redis
cp .env.example .env
docker compose up -d
docker compose ps
```

Ping:

```bash
redis-cli ping
```

If `REDIS_PASSWORD` is set:

```bash
REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli ping
```

## Streams Smoke Commands

Create the group:

```bash
redis-cli XGROUP CREATE stream:quotes:v1 cg:ktor:quotes '$' MKSTREAM
```

Publish a test message:

```bash
redis-cli XADD stream:quotes:v1 MAXLEN '~' 100000 '*' payload testdata
```

Read as Ktor instance 1:

```bash
redis-cli XREADGROUP GROUP cg:ktor:quotes ktor-instance-1 COUNT 10 STREAMS stream:quotes:v1 '>'
```

Ack a message:

```bash
redis-cli XACK stream:quotes:v1 cg:ktor:quotes '<message-id>'
```

Check stream length:

```bash
redis-cli XLEN stream:quotes:v1
```

## TTL Cache Smoke Commands

```bash
redis-cli SET 'cache:session:smoke' 'session-data' EX 1800
redis-cli SET 'cache:price:SBER' '252.00' EX 5
redis-cli SET 'cache:orderbook:SBER' 'orderbook-data' EX 2
redis-cli SET 'cache:user:last-active:42' '1775901000000' EX 600

redis-cli TTL 'cache:session:smoke'
redis-cli TTL 'cache:price:SBER'
redis-cli TTL 'cache:orderbook:SBER'
redis-cli TTL 'cache:user:last-active:42'
```

## Smoke Test Script

```bash
cd /Users/temo4ka/ITMO/sem5/tradingexchange/redis
chmod +x scripts/smoke-test.sh
./scripts/smoke-test.sh
```

With password:

```bash
REDIS_PASSWORD='<password>' ./scripts/smoke-test.sh
```

## Metrics

redis-exporter exposes metrics at:

```text
http://localhost:9121/metrics
```

Check expected metrics:

```bash
curl -fsS http://localhost:9121/metrics | grep -E '^(redis_up|redis_memory_used_bytes|redis_connected_clients|redis_instantaneous_ops_per_sec|redis_commands_processed_total|redis_keyspace_hits_total|redis_keyspace_misses_total|redis_evicted_keys_total)'
```

Minimum expected metrics:

- `redis_up`
- `redis_memory_used_bytes`
- `redis_connected_clients`
- `redis_instantaneous_ops_per_sec`
- `redis_commands_processed_total`
- `redis_keyspace_hits_total`
- `redis_keyspace_misses_total`
- `redis_evicted_keys_total`

## Integration Mismatches

- Ktor currently consumes Pub/Sub channel `quotes.ticks` and JSON messages in `RedisSubscriber`.
- Ktor currently stores latest prices in an in-process `ConcurrentHashMap`, not Redis keys like `cache:price:{symbol}`.
- Go currently uses `LogPublisher`; Redis Streams publishing is not implemented in runtime code yet.
- The Redis module contract is Streams plus opaque Protobuf bytes in field `payload`.

These are application integration issues outside this module's scope.
