package com.tradingexchange.app

import com.tradingexchange.app.data.remote.InstrumentDto
import com.tradingexchange.app.data.remote.OrderDto
import com.tradingexchange.app.data.remote.PortfolioPositionDto
import com.tradingexchange.app.data.remote.toDomain
import com.tradingexchange.app.domain.model.OrderSide
import com.tradingexchange.app.domain.model.OrderStatus
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapperTest {
    @Test
    fun portfolioPositionConvertsDecimalStringsAndNullablePrice() {
        val domain = PortfolioPositionDto(
            ticker = "SBER",
            quantity = "10.50000000",
            avgPrice = "250.1200",
            currentPrice = null,
            currency = "RUB",
        ).toDomain()

        assertEquals(BigDecimal("10.50000000"), domain.quantity)
        assertEquals(BigDecimal("250.1200"), domain.avgPrice)
        assertNull(domain.currentPrice)
    }

    @Test
    fun instrumentUsesCurrentKtorLastPriceField() {
        val domain = InstrumentDto(
            ticker = "SBER",
            name = "Sberbank",
            currency = "RUB",
            lotSize = 1,
            isActive = true,
            lastPrice = "252.0000",
        ).toDomain()

        assertEquals(BigDecimal("252.0000"), domain.lastPrice)
    }

    @Test
    fun orderMapsEnumsSafely() {
        val domain = OrderDto(
            orderId = "id",
            ticker = "SBER",
            side = "BUY",
            orderType = "MARKET",
            status = "FILLED",
            quantity = "1",
            executedPrice = "252.0000",
            createdAt = "2026-04-11T12:30:00Z",
        ).toDomain()

        assertEquals(OrderSide.BUY, domain.side)
        assertEquals(OrderStatus.FILLED, domain.status)
    }
}
