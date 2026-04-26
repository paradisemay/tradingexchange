package com.tradingexchange.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(val email: String, val password: String, val fullName: String? = null)

@Serializable
data class LoginRequestDto(val email: String, val password: String)

@Serializable
data class RefreshRequestDto(val refreshToken: String)

@Serializable
data class LogoutRequestDto(val refreshToken: String)

@Serializable
data class TokenPairDto(val accessToken: String, val refreshToken: String)

@Serializable
data class RegisterResponseDto(val userId: String, val accessToken: String, val refreshToken: String)

@Serializable
data class UserProfileDto(val userId: String, val email: String, val fullName: String? = null, val role: String)

@Serializable
data class PortfolioResponseDto(val positions: List<PortfolioPositionDto>, val cash: CashDto)

@Serializable
data class PortfolioPositionDto(
    val ticker: String,
    val quantity: String,
    val avgPrice: String,
    val currentPrice: String? = null,
    val currency: String,
)

@Serializable
data class CashDto(val currency: String, val available: String)

@Serializable
data class InstrumentDto(
    val ticker: String,
    val name: String,
    val currency: String,
    val lotSize: Int,
    val isActive: Boolean,
    val lastPrice: String? = null,
)

@Serializable
data class CreateOrderRequestDto(
    val ticker: String,
    val side: String,
    val orderType: String,
    val quantity: String,
    val limitPrice: String? = null,
)

@Serializable
data class OrderDto(
    val orderId: String,
    val ticker: String,
    val side: String,
    val orderType: String,
    val status: String,
    val quantity: String,
    val executedPrice: String? = null,
    val createdAt: String,
)

@Serializable
data class OrderListResponseDto(val orders: List<OrderDto>, val nextCursor: String? = null)

@Serializable
data class TransactionDto(
    val id: String,
    val type: String,
    val ticker: String? = null,
    val amount: String,
    val quantity: String? = null,
    val createdAt: String,
)

@Serializable
data class TransactionListResponseDto(val transactions: List<TransactionDto>, val nextCursor: String? = null)

@Serializable
data class ErrorResponseDto(
    val errorCode: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
    val traceId: String = "",
)

@Serializable
data class WsCommandDto(val type: String, val tickers: List<String>)

@Serializable
data class QuoteEventDto(
    val type: String,
    val ticker: String? = null,
    val price: String? = null,
    val currency: String? = null,
    val timestampMs: Long? = null,
    val errorCode: String? = null,
    val message: String? = null,
    val details: Map<String, String> = emptyMap(),
    val traceId: String = "",
)
