package com.example.plugins

import com.example.quotes.RedisSubscriber
import com.example.quotes.WebSocketManager
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.semconv.ServiceAttributes

fun Application.configureOpenTelemetry(): OpenTelemetry {
    val otel = buildOpenTelemetry("ktor-backend")
    install(KtorServerTelemetry) { setOpenTelemetry(otel) }
    return otel
}

fun registerMetrics(
    otel: OpenTelemetry,
    wsManager: WebSocketManager,
    redisSubscriber: RedisSubscriber,
    dataSource: HikariDataSource,
) {
    val meter = otel.getMeter("ktor-backend")

    meter.gaugeBuilder("ws_active_connections")
        .setDescription("Active WebSocket connections")
        .ofLongs()
        .buildWithCallback { it.record(wsManager.activeCount().toLong()) }

    meter.gaugeBuilder("redis_reconnect_count")
        .setDescription("Total Redis reconnect attempts since startup")
        .ofLongs()
        .buildWithCallback { it.record(redisSubscriber.reconnectCount) }

    meter.gaugeBuilder("db_pool_usage")
        .setDescription("Active JDBC connections in HikariCP pool")
        .ofLongs()
        .buildWithCallback { it.record(dataSource.hikariPoolMXBean?.activeConnections?.toLong() ?: 0L) }
}

private fun buildOpenTelemetry(serviceName: String): OpenTelemetry =
    AutoConfiguredOpenTelemetrySdk.builder()
        .addResourceCustomizer { resource, _ ->
            resource.toBuilder()
                .putAll(resource.attributes)
                .put(ServiceAttributes.SERVICE_NAME, serviceName)
                .build()
        }
        .build()
        .openTelemetrySdk
