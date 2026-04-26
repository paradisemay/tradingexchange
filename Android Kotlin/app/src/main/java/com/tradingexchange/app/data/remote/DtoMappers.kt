package com.tradingexchange.app.data.remote

import com.tradingexchange.app.domain.model.Cash
import com.tradingexchange.app.domain.model.CreateOrderCommand
import com.tradingexchange.app.domain.model.Instrument
import com.tradingexchange.app.domain.model.Order
import com.tradingexchange.app.domain.model.Portfolio
import com.tradingexchange.app.domain.model.PortfolioPosition
import com.tradingexchange.app.domain.model.Quote
import com.tradingexchange.app.domain.model.TokenPair
import com.tradingexchange.app.domain.model.Transaction
import com.tradingexchange.app.domain.model.UserProfile
import com.tradingexchange.app.domain.model.toMoney
import com.tradingexchange.app.domain.model.toOrderSide
import com.tradingexchange.app.domain.model.toOrderStatus
import com.tradingexchange.app.domain.model.toOrderType
import com.tradingexchange.app.domain.model.toTransactionType

fun TokenPairDto.toDomain() = TokenPair(accessToken, refreshToken)
fun RegisterResponseDto.toDomain() = TokenPair(accessToken, refreshToken)

fun UserProfileDto.toDomain() = UserProfile(userId, email, fullName, role)

fun PortfolioResponseDto.toDomain() = Portfolio(
    positions = positions.map { it.toDomain() },
    cash = Cash(cash.currency, cash.available.toMoney()),
)

fun PortfolioPositionDto.toDomain() = PortfolioPosition(
    ticker = ticker,
    quantity = quantity.toMoney(),
    avgPrice = avgPrice.toMoney(),
    currentPrice = currentPrice?.toMoney(),
    currency = currency,
)

fun InstrumentDto.toDomain() = Instrument(ticker, name, currency, lotSize, isActive, lastPrice?.toMoney())

fun CreateOrderCommand.toDto() = CreateOrderRequestDto(
    ticker = ticker,
    side = side.name,
    orderType = orderType.name,
    quantity = quantity.toPlainString(),
    limitPrice = limitPrice?.toPlainString(),
)

fun OrderDto.toDomain() = Order(
    orderId = orderId,
    ticker = ticker,
    side = side.toOrderSide(),
    orderType = orderType.toOrderType(),
    status = status.toOrderStatus(),
    quantity = quantity.toMoney(),
    executedPrice = executedPrice?.toMoney(),
    createdAt = createdAt,
)

fun TransactionDto.toDomain() = Transaction(
    id = id,
    type = type.toTransactionType(),
    ticker = ticker,
    amount = amount.toMoney(),
    quantity = quantity?.toMoney(),
    createdAt = createdAt,
)

fun QuoteEventDto.toDomainOrNull(): Quote? {
    if (type != "quote") return null
    val safeTicker = ticker ?: return null
    val safePrice = price ?: return null
    return Quote(
        ticker = safeTicker,
        price = safePrice.toMoney(),
        currency = currency ?: "RUB",
        timestampMs = timestampMs ?: 0L,
    )
}
