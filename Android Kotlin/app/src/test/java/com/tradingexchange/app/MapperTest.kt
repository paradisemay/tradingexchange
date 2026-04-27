package com.tradingexchange.app

import com.tradingexchange.app.data.remote.InstrumentDto
import com.tradingexchange.app.data.remote.CandleChartResponseDto
import com.tradingexchange.app.data.remote.CandleDto
import com.tradingexchange.app.data.remote.LineChartPointDto
import com.tradingexchange.app.data.remote.LineChartResponseDto
import com.tradingexchange.app.data.remote.OrderDto
import com.tradingexchange.app.data.remote.PortfolioPositionDto
import com.tradingexchange.app.data.remote.toDomain
import com.tradingexchange.app.domain.model.ChartRange
import com.tradingexchange.app.domain.model.isValidFor
import com.tradingexchange.app.domain.model.OrderSide
import com.tradingexchange.app.domain.model.OrderStatus
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapperTest {
    @Test
    fun chartShortRangesUseNonAmbiguousApiValues() {
        assertEquals("1MIN", ChartRange.MINUTE.apiValue)
        assertEquals("1m", ChartRange.MINUTE.label)
        assertEquals("1H", ChartRange.HOUR.apiValue)
        assertEquals("1h", ChartRange.HOUR.label)
        assertEquals("1M", ChartRange.MONTH.apiValue)
    }

    @Test
    fun chartIntervalMustBeSmallerThanRange() {
        assertEquals(true, com.tradingexchange.app.domain.model.ChartInterval.SECOND.isValidFor(ChartRange.MINUTE))
        assertEquals(false, com.tradingexchange.app.domain.model.ChartInterval.MINUTE.isValidFor(ChartRange.MINUTE))
        assertEquals(false, com.tradingexchange.app.domain.model.ChartInterval.HOUR.isValidFor(ChartRange.HOUR))
        assertEquals(false, com.tradingexchange.app.domain.model.ChartInterval.DAY.isValidFor(ChartRange.DAY))
    }

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

    @Test
    fun lineChartMapsDecimalStrings() {
        val domain = LineChartResponseDto(
            ticker = "SBER",
            currency = "RUB",
            range = "1D",
            interval = "5m",
            points = listOf(LineChartPointDto(timestampMs = 1L, price = "252.4200")),
        ).toDomain()

        assertEquals(BigDecimal("252.4200"), domain.points.first().price)
    }

    @Test
    fun candleChartMapsOhlcDecimalStrings() {
        val domain = CandleChartResponseDto(
            ticker = "SBER",
            currency = "RUB",
            range = "1D",
            interval = "5m",
            candles = listOf(CandleDto(timestampMs = 1L, open = "251.0000", high = "253.0000", low = "250.5000", close = "252.0000")),
        ).toDomain()

        assertEquals(BigDecimal("253.0000"), domain.candles.first().high)
        assertEquals(BigDecimal("250.5000"), domain.candles.first().low)
    }
}
