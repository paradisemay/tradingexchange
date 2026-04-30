#!/usr/bin/env bash

set -euo pipefail

curl -fsS http://localhost:13133/ >/dev/null
curl -fsS http://localhost:9090/-/ready >/dev/null
curl -fsS http://localhost:3100/ready >/dev/null
curl -fsS http://localhost:3200/ready >/dev/null
curl -fsS http://localhost:8889/metrics >/dev/null
curl -fsS http://localhost:9121/metrics | grep -E '^redis_up |^redis_memory_used_bytes |^redis_connected_clients |^redis_instantaneous_ops_per_sec |^redis_commands_processed_total |^redis_keyspace_hits_total |^redis_keyspace_misses_total |^redis_evicted_keys_total '
curl -fsS http://localhost:9187/metrics | grep -E '^pg_up |^pg_stat_database_numbackends |^pg_stat_database_xact_commit |^pg_stat_database_xact_rollback |^pg_locks_count |^pg_stat_activity_count |^pg_database_size_bytes '

echo "telemetry smoke checks passed"
