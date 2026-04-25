package com.example.plugins

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import io.opentelemetry.semconv.ServiceAttributes
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry

fun Application.configureOpenTelemetry() {
    install(KtorServerTelemetry) {
        setOpenTelemetry(buildOpenTelemetry("ktor-backend"))
    }
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
