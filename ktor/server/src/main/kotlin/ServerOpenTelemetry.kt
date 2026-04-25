package com.example

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry

fun Application.configureOpenTelemetry() {
    install(KtorServerTelemetry) {
        setOpenTelemetry(getOpenTelemetry(serviceName = "ktor-backend"))
    }
}
