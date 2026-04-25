package com.example.quotes

import com.example.config.RedisConfig
import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class RedisSubscriber(
    private val config: RedisConfig,
    private val priceCache: PriceCache,
    private val wsManager: WebSocketManager,
    private val json: Json,
) {
    private val log = LoggerFactory.getLogger(RedisSubscriber::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var redisClient: RedisClient? = null
    private var pubSubConn: StatefulRedisPubSubConnection<String, String>? = null

    @Volatile var reconnectCount = 0L
        private set

    fun start() {
        scope.launch { connectWithRetry() }
    }

    fun stop() {
        pubSubConn?.close()
        redisClient?.shutdown()
        scope.cancel()
    }

    private suspend fun connectWithRetry() {
        val backoffs = listOf(1L, 2L, 5L, 10L, 30L)
        var attempt = 0
        while (true) {
            try {
                connect()
                log.info("Redis subscriber connected to quotes.ticks")
                break
            } catch (e: Exception) {
                reconnectCount++
                val delaySeconds = backoffs.getOrElse(attempt) { 30L }
                log.warn("Redis connection failed (attempt $attempt), retrying in ${delaySeconds}s: ${e.message}")
                delay(delaySeconds * 1000)
                attempt++
            }
        }
    }

    private fun connect() {
        val client = RedisClient.create(config.uri)
        val conn = client.connectPubSub()
        conn.addListener(object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String, message: String) = handleMessage(message)
            override fun unsubscribed(channel: String, count: Long) {
                log.warn("Unsubscribed from $channel; reconnecting")
                scope.launch { reconnect() }
            }
        })
        conn.sync().subscribe("quotes.ticks")
        redisClient = client
        pubSubConn = conn
    }

    private suspend fun reconnect() {
        pubSubConn?.close()
        redisClient?.shutdown()
        reconnectCount++
        connectWithRetry()
    }

    private fun handleMessage(message: String) {
        runCatching {
            val quote = json.decodeFromString<QuoteMessage>(message)
            priceCache.update(quote)
            scope.launch { wsManager.broadcast(quote, json) }
        }.onFailure { log.error("Failed to process quote message: $message", it) }
    }
}
