package com.tradingexchange.app.domain.model

import java.math.BigDecimal

fun String.toMoney(): BigDecimal = toBigDecimalOrNull() ?: BigDecimal.ZERO

fun String.toOrderSide(): OrderSide = runCatching { OrderSide.valueOf(this) }.getOrDefault(OrderSide.UNKNOWN)
fun String.toOrderType(): OrderType = runCatching { OrderType.valueOf(this) }.getOrDefault(OrderType.UNKNOWN)
fun String.toOrderStatus(): OrderStatus = runCatching { OrderStatus.valueOf(this) }.getOrDefault(OrderStatus.UNKNOWN)
fun String.toTransactionType(): TransactionType = runCatching { TransactionType.valueOf(this) }.getOrDefault(TransactionType.UNKNOWN)

fun AppError.userMessage(): String = when (this) {
    AppError.InsufficientFunds -> "Недостаточно денег для сделки"
    AppError.InsufficientPosition -> "Недостаточно бумаг для продажи"
    AppError.Network -> "Нет соединения с сервером"
    AppError.NotFound -> "Инструмент не найден"
    AppError.QuoteUnavailable -> "Цена временно недоступна"
    AppError.Unauthorized -> "Нужно войти заново"
    AppError.Validation -> "Проверьте введенные данные"
    is AppError.Unknown -> message
}
