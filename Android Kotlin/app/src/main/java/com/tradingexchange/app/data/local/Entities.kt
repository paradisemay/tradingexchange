package com.tradingexchange.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "portfolio_positions")
data class PortfolioPositionEntity(
    @PrimaryKey val ticker: String,
    val quantity: String,
    val avgPrice: String,
    val currentPrice: String?,
    val currency: String,
)

@Entity(tableName = "cash")
data class CashEntity(@PrimaryKey val currency: String, val available: String)

@Entity(tableName = "instruments")
data class InstrumentEntity(
    @PrimaryKey val ticker: String,
    val name: String,
    val currency: String,
    val lotSize: Int,
    val isActive: Boolean,
    val lastPrice: String?,
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val orderId: String,
    val ticker: String,
    val side: String,
    val orderType: String,
    val status: String,
    val quantity: String,
    val executedPrice: String?,
    val createdAt: String,
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val ticker: String?,
    val amount: String,
    val quantity: String?,
    val createdAt: String,
)

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val ticker: String,
    val price: String,
    val currency: String,
    val timestampMs: Long,
)
