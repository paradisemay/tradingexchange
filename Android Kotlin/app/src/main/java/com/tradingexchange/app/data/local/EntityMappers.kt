package com.tradingexchange.app.data.local

import com.tradingexchange.app.domain.model.Cash
import com.tradingexchange.app.domain.model.Instrument
import com.tradingexchange.app.domain.model.Order
import com.tradingexchange.app.domain.model.Portfolio
import com.tradingexchange.app.domain.model.PortfolioPosition
import com.tradingexchange.app.domain.model.Quote
import com.tradingexchange.app.domain.model.Transaction
import com.tradingexchange.app.domain.model.toMoney
import com.tradingexchange.app.domain.model.toOrderSide
import com.tradingexchange.app.domain.model.toOrderStatus
import com.tradingexchange.app.domain.model.toOrderType
import com.tradingexchange.app.domain.model.toTransactionType

fun Portfolio.toEntities(): Pair<List<PortfolioPositionEntity>, CashEntity> =
    positions.map { it.toEntity() } to CashEntity(cash.currency, cash.available.toPlainString())

fun PortfolioPosition.toEntity() = PortfolioPositionEntity(
    ticker, quantity.toPlainString(), avgPrice.toPlainString(), currentPrice?.toPlainString(), currency,
)

fun PortfolioPositionEntity.toDomain() = PortfolioPosition(ticker, quantity.toMoney(), avgPrice.toMoney(), currentPrice?.toMoney(), currency)

fun CashEntity.toDomain() = Cash(currency, available.toMoney())

fun Instrument.toEntity() = InstrumentEntity(ticker, name, currency, lotSize, isActive, lastPrice?.toPlainString())

fun InstrumentEntity.toDomain() = Instrument(ticker, name, currency, lotSize, isActive, lastPrice?.toMoney())

fun Order.toEntity() = OrderEntity(orderId, ticker, side.name, orderType.name, status.name, quantity.toPlainString(), executedPrice?.toPlainString(), createdAt)

fun OrderEntity.toDomain() = Order(orderId, ticker, side.toOrderSide(), orderType.toOrderType(), status.toOrderStatus(), quantity.toMoney(), executedPrice?.toMoney(), createdAt)

fun Transaction.toEntity() = TransactionEntity(id, type.name, ticker, amount.toPlainString(), quantity?.toPlainString(), createdAt)

fun TransactionEntity.toDomain() = Transaction(id, type.toTransactionType(), ticker, amount.toMoney(), quantity?.toMoney(), createdAt)

fun Quote.toEntity() = QuoteEntity(ticker, price.toPlainString(), currency, timestampMs)
