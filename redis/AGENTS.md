# Redis Module Agents

## Scope

This file applies only to the Redis module.

The project already has Go and Ktor modules. Do not implement Go or Ktor.
Redis must provide:

- Redis Streams for hot quotes transport.
- In-memory cache for short-lived Ktor data.
- Redis exporter for telemetry.
- Docker-based local startup.

## Agent: redis-architect

Role: Redis architecture owner.

Responsibilities:

- Design Redis Streams contract.
- Use stream name: stream:quotes:v1.
- Use Ktor consumer group: cg:ktor:quotes.
- Use consumer name pattern: ktor-instance-{n}.
- Store quote payload as binary Protobuf bytes in field payload.
- Do not validate business fields inside Redis.
- Define cache keyspace:
  - cache:session:{sessionId}, TTL 30m
  - cache:price:{symbol}, TTL 5s
  - cache:orderbook:{symbol}, TTL 1-2s
  - cache:user:last-active:{userId}, TTL 10m
- Ensure all cache keys have TTL.
- Use XADD MAXLEN ~ 100000 for quote stream.
- Use AOF everysec and optional RDB snapshots.
- Use maxmemory-policy volatile-ttl.

## Agent: redis-devops

Role: Redis infrastructure implementer.

Responsibilities:

- Create or update Redis docker-compose files.
- Add Redis 7.x container.
- Add redis-exporter container.
- Add Redis volume for AOF/RDB persistence.
- Configure redis.conf:
  - appendonly yes
  - appendfsync everysec
  - maxmemory from environment or documented value
  - maxmemory-policy volatile-ttl
  - save snapshots if needed
- Add healthcheck using redis-cli ping.
- Use password from environment variable, no hardcode.
- Expose Redis only for local development when needed.
- Expose redis-exporter metrics on port 9121.
- Keep configuration simple and suitable for a study project.

## Agent: redis-integration-tester

Role: Redis verification and documentation owner.

Responsibilities:

- Add smoke commands or scripts to verify Redis.
- Verify Redis responds to PING.
- Verify stream write:
  XADD stream:quotes:v1 MAXLEN ~ 100000 \* payload <protobuf-bytes-or-test-value>
- Verify consumer group:
  XGROUP CREATE stream:quotes:v1 cg:ktor:quotes $ MKSTREAM
  XREADGROUP GROUP cg:ktor:quotes ktor-instance-1 COUNT 10 STREAMS stream:quotes:v1 >
  XACK stream:quotes:v1 cg:ktor:quotes <message-id>
- Verify TTL cache keys.
- Verify stream length does not grow forever.
- Verify redis-exporter exposes metrics.
- Write README instructions for local launch and checks.

## Working rules

- First inspect the existing project structure.
- Then propose a plan before editing files.
- Work only inside redis unless explicit integration changes are required.
- Do not modify Go, Ktor, proto, PostgreSQL, ClickHouse, or frontend without explicit request.
- If neighboring modules have inconsistent contracts, report the inconsistency before changing anything.
- After implementation, list changed files and exact commands to test.
