package com.example.quotes

import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WebSocketManager {
    private val sessions = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val subscriptions = ConcurrentHashMap<String, MutableSet<String>>()

    fun addSession(sessionId: String, session: DefaultWebSocketSession) {
        sessions[sessionId] = session
        subscriptions[sessionId] = ConcurrentHashMap.newKeySet()
    }

    fun removeSession(sessionId: String) {
        sessions.remove(sessionId)
        subscriptions.remove(sessionId)
    }

    fun subscribe(sessionId: String, tickers: List<String>) {
        subscriptions[sessionId]?.addAll(tickers)
    }

    fun unsubscribe(sessionId: String, tickers: List<String>) {
        subscriptions[sessionId]?.removeAll(tickers.toSet())
    }

    suspend fun broadcast(quote: QuoteMessage, json: Json) {
        val event = QuoteEvent(ticker = quote.ticker, price = quote.price, currency = quote.currency, timestampMs = quote.timestampMs)
        val text = json.encodeToString(event)
        sessions.forEach { (sessionId, session) ->
            if (subscriptions[sessionId]?.contains(quote.ticker) == true) {
                runCatching { session.send(Frame.Text(text)) }
            }
        }
    }

    fun activeCount(): Int = sessions.size

    fun newSessionId(): String = UUID.randomUUID().toString()
}
