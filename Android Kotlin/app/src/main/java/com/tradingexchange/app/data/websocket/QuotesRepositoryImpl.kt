package com.tradingexchange.app.data.websocket

import com.tradingexchange.app.data.local.BrokerDao
import com.tradingexchange.app.data.local.TokenStore
import com.tradingexchange.app.data.local.toEntity
import com.tradingexchange.app.data.remote.QuoteEventDto
import com.tradingexchange.app.data.remote.WsCommandDto
import com.tradingexchange.app.data.remote.toDomainOrNull
import com.tradingexchange.app.domain.model.Quote
import com.tradingexchange.app.domain.repository.QuotesRepository
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber

@Singleton
class QuotesRepositoryImpl @Inject constructor(
    @Named("mainClient") private val okHttpClient: OkHttpClient,
    @Named("wsUrl") private val wsUrl: String,
    private val tokenStore: TokenStore,
    private val dao: BrokerDao,
    private val json: Json,
) : QuotesRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeSubscriptions = MutableStateFlow<Set<String>>(emptySet())
    private val mutableQuotes = MutableStateFlow<Map<String, Quote>>(emptyMap())
    private val mutableConnected = MutableStateFlow(false)
    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var manualDisconnect = false

    override val quotes: StateFlow<Map<String, Quote>> = mutableQuotes
    override val isConnected: StateFlow<Boolean> = mutableConnected

    override fun connect() {
        manualDisconnect = false
        openSocket()
    }

    override fun disconnect() {
        manualDisconnect = true
        reconnectJob?.cancel()
        socket?.close(1000, "client logout")
        socket = null
        mutableConnected.value = false
    }

    override fun subscribe(tickers: Set<String>) {
        val normalized = tickers.map { it.uppercase() }.toSet()
        activeSubscriptions.value = activeSubscriptions.value + normalized
        sendCommand("subscribe", normalized)
    }

    override fun unsubscribe(tickers: Set<String>) {
        val normalized = tickers.map { it.uppercase() }.toSet()
        activeSubscriptions.value = activeSubscriptions.value - normalized
        sendCommand("unsubscribe", normalized)
    }

    private fun openSocket() {
        scope.launch {
            val token = tokenStore.accessToken.first()
            val url = if (token.isNullOrBlank()) wsUrl else "$wsUrl?accessToken=$token"
            val request = Request.Builder().url(url).build()
            socket = okHttpClient.newWebSocket(request, listener())
        }
    }

    private fun listener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            socket = webSocket
            mutableConnected.value = true
            val current = activeSubscriptions.value
            if (current.isNotEmpty()) sendCommand("subscribe", current)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val event = runCatching { json.decodeFromString(QuoteEventDto.serializer(), text) }.getOrNull()
            val quote = event?.toDomainOrNull() ?: return
            mutableQuotes.value = mutableQuotes.value + (quote.ticker to quote)
            scope.launch { dao.upsertQuote(quote.toEntity()) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Timber.w(t, "Quotes websocket failed")
            mutableConnected.value = false
            if (!manualDisconnect) scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            mutableConnected.value = false
            if (!manualDisconnect) scheduleReconnect()
        }
    }

    private fun sendCommand(type: String, tickers: Set<String>) {
        if (tickers.isEmpty()) return
        val command = WsCommandDto(type, tickers.toList())
        socket?.send(json.encodeToString(command))
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            val delays = listOf(1_000L, 2_000L, 5_000L, 10_000L, 30_000L)
            var index = 0
            while (!manualDisconnect && !mutableConnected.value) {
                delay(delays[index.coerceAtMost(delays.lastIndex)])
                openSocket()
                index++
            }
        }
    }
}
