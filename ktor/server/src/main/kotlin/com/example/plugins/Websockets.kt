package com.example.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets

fun Application.configureWebsockets() {
    install(WebSockets) {
        pingPeriodMillis = 30_000
        timeoutMillis = 30_000
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}
