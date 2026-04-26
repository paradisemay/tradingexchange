#!/usr/bin/env sh
set -eu

REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_STREAM="${REDIS_STREAM:-stream:quotes:v1}"
REDIS_CONSUMER_GROUP="${REDIS_CONSUMER_GROUP:-cg:ktor:quotes}"
REDIS_CONSUMER_NAME="${REDIS_CONSUMER_NAME:-ktor-instance-1}"
REDIS_STREAM_MAXLEN="${REDIS_STREAM_MAXLEN:-100000}"
REDIS_EXPORTER_URL="${REDIS_EXPORTER_URL:-http://localhost:9121/metrics}"

redis_cli() {
  if [ -n "${REDIS_PASSWORD:-}" ]; then
    REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" "$@"
  else
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" "$@"
  fi
}

echo "1. PING"
redis_cli ping

echo "2. Consumer group"
if redis_cli XGROUP CREATE "$REDIS_STREAM" "$REDIS_CONSUMER_GROUP" '$' MKSTREAM; then
  echo "created group $REDIS_CONSUMER_GROUP"
else
  echo "group $REDIS_CONSUMER_GROUP already exists or Redis returned a non-fatal BUSYGROUP"
fi

echo "3. XADD with approximate MAXLEN"
message_id="$(redis_cli XADD "$REDIS_STREAM" MAXLEN '~' "$REDIS_STREAM_MAXLEN" '*' payload testdata)"
echo "message_id=$message_id"

echo "4. XREADGROUP"
redis_cli XREADGROUP GROUP "$REDIS_CONSUMER_GROUP" "$REDIS_CONSUMER_NAME" COUNT 10 STREAMS "$REDIS_STREAM" '>'

echo "5. XACK"
redis_cli XACK "$REDIS_STREAM" "$REDIS_CONSUMER_GROUP" "$message_id"

echo "6. Cache TTL checks"
redis_cli SET 'cache:session:smoke' 'session-data' EX 1800
redis_cli SET 'cache:price:SBER' '252.00' EX 5
redis_cli SET 'cache:orderbook:SBER' 'orderbook-data' EX 2
redis_cli SET 'cache:user:last-active:42' '1775901000000' EX 600
redis_cli TTL 'cache:session:smoke'
redis_cli TTL 'cache:price:SBER'
redis_cli TTL 'cache:orderbook:SBER'
redis_cli TTL 'cache:user:last-active:42'

echo "7. Stream length"
redis_cli XLEN "$REDIS_STREAM"

echo "8. redis-exporter metrics"
if command -v curl >/dev/null 2>&1; then
  curl -fsS "$REDIS_EXPORTER_URL" | grep -E '^(redis_up|redis_memory_used_bytes|redis_connected_clients|redis_instantaneous_ops_per_sec|redis_commands_processed_total|redis_keyspace_hits_total|redis_keyspace_misses_total|redis_evicted_keys_total)' >/dev/null
  echo "metrics ok: $REDIS_EXPORTER_URL"
else
  echo "curl not found; open $REDIS_EXPORTER_URL and check redis_up and memory/client/keyspace metrics"
fi
