package com.example.domain

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val fullName: String?,
    val role: String,
    val createdAt: Instant,
)

data class Account(
    val id: UUID,
    val userId: UUID,
    val currency: String,
    val cashBalance: BigDecimal,
    val reservedBalance: BigDecimal,
)

data class Instrument(
    val ticker: String,
    val name: String,
    val currency: String,
    val lotSize: Int,
    val isActive: Boolean,
    val lastPrice: BigDecimal?,
)

data class PortfolioPosition(
    val userId: UUID,
    val ticker: String,
    val quantity: BigDecimal,
    val avgPrice: BigDecimal,
    val updatedAt: Instant,
)

data class Order(
    val id: UUID,
    val userId: UUID,
    val ticker: String,
    val side: String,
    val orderType: String,
    val status: String,
    val quantity: BigDecimal,
    val price: BigDecimal?,
    val executedPrice: BigDecimal?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class Transaction(
    val id: UUID,
    val userId: UUID,
    val orderId: UUID?,
    val ticker: String?,
    val type: String,
    val amount: BigDecimal,
    val quantity: BigDecimal?,
    val createdAt: Instant,
)

data class Session(
    val id: UUID,
    val userId: UUID,
    val refreshTokenHash: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
)
