package com.example.quotes

import com.example.auth.JwtUtil
import com.example.domain.AppException
import com.example.domain.ErrorCode
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

@Serializable
private data class WsClientMessage(val type: String, val tickers: List<String> = emptyList())

fun Route.quotesWebSocket(wsManager: WebSocketManager, jwtUtil: JwtUtil, json: Json) {
    val log = LoggerFactory.getLogger("QuotesWebSocket")

    webSocket("/api/v1/quotes/ws") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            ?: call.request.queryParameters["accessToken"]

        val userId = token?.let { jwtUtil.extractUserId(it) }
        if (userId == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
            return@webSocket
        }

        val sessionId = wsManager.newSessionId()
        wsManager.addSession(sessionId, this)
        log.info("WS session $sessionId connected for user $userId")

        try {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) handleClientFrame(frame.readText(), sessionId, wsManager, json)
            }
        } finally {
            wsManager.removeSession(sessionId)
            log.info("WS session $sessionId disconnected")
        }
    }
}

private fun handleClientFrame(text: String, sessionId: String, wsManager: WebSocketManager, json: Json) {
    runCatching {
        val obj = json.parseToJsonElement(text).jsonObject
        val type = obj["type"]?.jsonPrimitive?.content ?: return
        val tickers = obj["tickers"]?.let {
            json.decodeFromJsonElement(kotlinx.serialization.json.JsonArray.serializer(), it as kotlinx.serialization.json.JsonArray)
        }?.map { it.jsonPrimitive.content } ?: emptyList()

        when (type) {
            "subscribe" -> wsManager.subscribe(sessionId, tickers)
            "unsubscribe" -> wsManager.unsubscribe(sessionId, tickers)
        }
    }
}
