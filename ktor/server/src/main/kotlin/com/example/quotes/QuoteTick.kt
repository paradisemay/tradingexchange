package com.example.quotes

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Protobuf-десериализация сообщения из stream:quotes:v1 (поле payload).
 * Номера полей должны совпадать со схемой quotes.v1 Go-модуля.
 * TODO: сверить @ProtoNumber с .proto-файлом Go-команды когда он будет опубликован.
 */
@Serializable
data class QuoteTick(
    @ProtoNumber(1) val ticker: String = "",
    @ProtoNumber(2) val price: Float = 0f,
    @ProtoNumber(3) val timestampMs: Long = 0L,
    @ProtoNumber(4) val currency: String = "RUB",
)

fun QuoteTick.toQuoteMessage() = QuoteMessage(
    ticker = ticker,
    price = BigDecimal(price.toDouble()).setScale(4, RoundingMode.HALF_UP).toPlainString(),
    currency = currency.ifBlank { "RUB" },
    timestampMs = if (timestampMs > 0) timestampMs else System.currentTimeMillis(),
)
