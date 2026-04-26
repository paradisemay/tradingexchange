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
