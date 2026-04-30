package com.example.quotes

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.math.BigDecimal
import java.math.RoundingMode

// Protobuf contract: quotes.v1.proto (Go module)
// field 1 ticker string, field 2 price double, field 3 timestamp_ms int64
@Serializable
data class QuoteTick(
    @ProtoNumber(1) val ticker: String = "",
    @ProtoNumber(2) val price: Double = 0.0,
    @ProtoNumber(3) val timestampMs: Long = 0L,
)

fun QuoteTick.toQuoteMessage() = QuoteMessage(
    ticker = ticker,
    price = BigDecimal(price).setScale(4, RoundingMode.HALF_UP).toPlainString(),
    currency = "RUB",
    timestampMs = if (timestampMs > 0) timestampMs else System.currentTimeMillis(),
)
