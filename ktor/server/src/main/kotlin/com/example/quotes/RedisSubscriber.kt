package com.example.quotes

import com.example.config.RedisConfig
import io.lettuce.core.Consumer
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs
import io.lettuce.core.XReadArgs.StreamOffset
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import org.slf4j.LoggerFactory
import java.time.Duration

class RedisSubscriber(
    private val config: RedisConfig,
    private val priceCache: PriceCache,
    private val wsManager: WebSocketManager,
    private val json: Json,
) {
    private val log = LoggerFactory.getLogger(RedisSubscriber::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile var reconnectCount = 0L
        private set

    fun start() {
        scope.launch { runWithRetry() }
    }

    fun stop() {
        scope.cancel()
    }

    private suspend fun runWithRetry() {
        val backoffs = listOf(1L, 2L, 5L, 10L, 30L)
        var attempt = 0
        while (scope.isActive) {
            try {
                consume()
            } catch (e: Exception) {
                if (!scope.isActive) break
                reconnectCount++
                val delaySeconds = backoffs.getOrElse(attempt) { 30L }
                log.warn("Redis Streams error (attempt=$attempt), retrying in ${delaySeconds}s: ${e.message}")
                delay(delaySeconds * 1_000)
                attempt = minOf(attempt + 1, backoffs.size)
            }
        }
    }

    private fun consume() {
        val client = RedisClient.create(config.uri)
        // ByteArray-codec для значений — payload содержит бинарный Protobuf
        val codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        val conn = client.connect(codec)
        val sync = conn.sync()

        try {
            ensureConsumerGroup(sync)
            log.info("Redis Streams consumer ready: stream=${config.streamName} group=${config.consumerGroup} consumer=${config.consumerName}")

            while (scope.isActive) {
                val messages = sync.xreadgroup(
                    Consumer.from(config.consumerGroup, config.consumerName),
                    XReadArgs.Builder.count(100).block(Duration.ofMillis(2_000)),
                    StreamOffset.lastConsumed<String>(config.streamName),
                )
                messages?.forEach { msg ->
                    val payload = msg.body["payload"]
                    if (payload != null) handlePayload(payload)
                    // XACK независимо от успеха декодирования — не блокируем стрим
                    sync.xack(config.streamName, config.consumerGroup, msg.id)
                }
            }
        } finally {
            conn.close()
            client.shutdown()
        }
    }

    private fun ensureConsumerGroup(sync: io.lettuce.core.api.sync.RedisCommands<String, ByteArray>) {
        try {
            sync.xgroupCreate(
                StreamOffset.from<String>(config.streamName, "$"),
                config.consumerGroup,
                XGroupCreateArgs.Builder.mkstream(),
            )
        } catch (e: RedisCommandExecutionException) {
            // BUSYGROUP — группа уже существует, это нормально
            if (!e.message.orEmpty().contains("BUSYGROUP")) throw e
        }
    }

    private fun handlePayload(bytes: ByteArray) {
        val tick = runCatching {
            ProtoBuf.decodeFromByteArray(QuoteTick.serializer(), bytes)
        }.onFailure {
            log.debug("Protobuf decode failed (${bytes.size} bytes) — skipping: ${it.message}")
        }.getOrNull() ?: return

        if (tick.ticker.isBlank()) return

        val quote = tick.toQuoteMessage()
        priceCache.update(quote)
        scope.launch { wsManager.broadcast(quote, json) }
    }
}
