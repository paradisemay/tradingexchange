package com.example.quotes

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class QuoteMessage(
    val ticker: String,
    val price: String,
    val currency: String,
    val timestampMs: Long,
    val source: String = "unknown",
)

@Serializable
data class QuoteEvent(
    val type: String = "quote",
    val ticker: String,
    val price: String,
    val currency: String,
    val timestampMs: Long,
)

class PriceCache {
    private val prices = ConcurrentHashMap<String, QuoteMessage>()

    fun update(quote: QuoteMessage) {
        prices[quote.ticker] = quote
    }

    fun getPrice(ticker: String): BigDecimal? = prices[ticker]?.price?.toBigDecimalOrNull()

    fun getQuote(ticker: String): QuoteMessage? = prices[ticker]

    fun toEvent(q: QuoteMessage) = QuoteEvent(
        ticker = q.ticker,
        price = q.price,
        currency = q.currency,
        timestampMs = q.timestampMs,
    )
}
