package com.tradingexchange.app.domain.model

import java.math.BigDecimal

data class TokenPair(val accessToken: String, val refreshToken: String)

data class UserProfile(
    val userId: String,
    val email: String,
    val fullName: String?,
    val role: String,
)

data class Portfolio(
    val positions: List<PortfolioPosition>,
    val cash: Cash,
)

data class PortfolioPosition(
    val ticker: String,
    val quantity: BigDecimal,
    val avgPrice: BigDecimal,
    val currentPrice: BigDecimal?,
    val currency: String,
)

data class Cash(val currency: String, val available: BigDecimal)

data class Instrument(
    val ticker: String,
    val name: String,
    val currency: String,
    val lotSize: Int,
    val isActive: Boolean,
    val lastPrice: BigDecimal?,
)

enum class ChartRange(val apiValue: String, val label: String) {
    MINUTE("1MIN", "1m"),
    HOUR("1H", "1h"),
    DAY("1D", "1D"),
    WEEK("1W", "1W"),
    MONTH("1M", "1M"),
    SIX_MONTHS("6M", "6M"),
    YEAR("1Y", "1Y"),
}

fun ChartRange.toMillis(): Long =
    when (this) {
        ChartRange.MINUTE -> 60_000L
        ChartRange.HOUR -> 60 * 60_000L
        ChartRange.DAY -> 24 * 60 * 60_000L
        ChartRange.WEEK -> 7 * 24 * 60 * 60_000L
        ChartRange.MONTH -> 30L * 24 * 60 * 60_000L
        ChartRange.SIX_MONTHS -> 182L * 24 * 60 * 60_000L
        ChartRange.YEAR -> 365L * 24 * 60 * 60_000L
    }

enum class ChartInterval(val apiValue: String, val label: String) {
    SECOND("1s", "1s"),
    MINUTE("1m", "1m"),
    FIVE_MINUTES("5m", "5m"),
    FIFTEEN_MINUTES("15m", "15m"),
    HOUR("1h", "1h"),
    DAY("1d", "1d"),
}

fun ChartInterval.toMillis(): Long =
    when (this) {
        ChartInterval.SECOND -> 1_000L
        ChartInterval.MINUTE -> 60_000L
        ChartInterval.FIVE_MINUTES -> 5 * 60_000L
        ChartInterval.FIFTEEN_MINUTES -> 15 * 60_000L
        ChartInterval.HOUR -> 60 * 60_000L
        ChartInterval.DAY -> 24 * 60 * 60_000L
    }

fun ChartInterval.isValidFor(range: ChartRange): Boolean = toMillis() < range.toMillis()

fun ChartRange.recommendedInterval(): ChartInterval =
    when (this) {
        ChartRange.MINUTE -> ChartInterval.SECOND
        ChartRange.HOUR -> ChartInterval.MINUTE
        ChartRange.DAY -> ChartInterval.FIVE_MINUTES
        ChartRange.WEEK -> ChartInterval.HOUR
        ChartRange.MONTH,
        ChartRange.SIX_MONTHS,
        ChartRange.YEAR -> ChartInterval.DAY
    }

enum class ChartType { LINE, CANDLES }

data class LineChart(
    val ticker: String,
    val currency: String,
    val range: String,
    val interval: String,
    val points: List<LineChartPoint>,
)

data class LineChartPoint(
    val timestampMs: Long,
    val price: BigDecimal,
)

data class CandleChart(
    val ticker: String,
    val currency: String,
    val range: String,
    val interval: String,
    val candles: List<Candle>,
)

data class Candle(
    val timestampMs: Long,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
)

enum class OrderSide { BUY, SELL, UNKNOWN }
enum class OrderType { MARKET, LIMIT, UNKNOWN }
enum class OrderStatus { NEW, FILLED, CANCELLED, REJECTED, UNKNOWN }
enum class TransactionType { DEPOSIT, WITHDRAW, BUY, SELL, FEE, UNKNOWN }

data class CreateOrderCommand(
    val ticker: String,
    val side: OrderSide,
    val orderType: OrderType,
    val quantity: BigDecimal,
    val limitPrice: BigDecimal?,
)

data class Order(
    val orderId: String,
    val ticker: String,
    val side: OrderSide,
    val orderType: OrderType,
    val status: OrderStatus,
    val quantity: BigDecimal,
    val executedPrice: BigDecimal?,
    val createdAt: String,
)

data class Transaction(
    val id: String,
    val type: TransactionType,
    val ticker: String?,
    val amount: BigDecimal,
    val quantity: BigDecimal?,
    val createdAt: String,
)

data class Quote(
    val ticker: String,
    val price: BigDecimal,
    val currency: String,
    val timestampMs: Long,
)

sealed interface AppError {
    data object Unauthorized : AppError
    data object Validation : AppError
    data object InsufficientFunds : AppError
    data object InsufficientPosition : AppError
    data object QuoteUnavailable : AppError
    data object NotFound : AppError
    data object Network : AppError
    data class Unknown(val message: String) : AppError
}

data class ResultPage<T>(val items: List<T>, val nextCursor: String?)
