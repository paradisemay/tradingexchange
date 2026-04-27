package com.tradingexchange.app

import com.tradingexchange.app.domain.model.Candle
import com.tradingexchange.app.domain.model.CandleChart
import com.tradingexchange.app.domain.model.Cash
import com.tradingexchange.app.domain.model.ChartInterval
import com.tradingexchange.app.domain.model.ChartRange
import com.tradingexchange.app.domain.model.ChartType
import com.tradingexchange.app.domain.model.CreateOrderCommand
import com.tradingexchange.app.domain.model.Instrument
import com.tradingexchange.app.domain.model.LineChart
import com.tradingexchange.app.domain.model.LineChartPoint
import com.tradingexchange.app.domain.model.Order
import com.tradingexchange.app.domain.model.Portfolio
import com.tradingexchange.app.domain.model.Quote
import com.tradingexchange.app.domain.model.ResultPage
import com.tradingexchange.app.domain.model.Transaction
import com.tradingexchange.app.domain.model.UserProfile
import com.tradingexchange.app.domain.repository.BrokerRepository
import com.tradingexchange.app.domain.repository.QuotesRepository
import com.tradingexchange.app.domain.usecase.GetCandleChartUseCase
import com.tradingexchange.app.domain.usecase.GetLineChartUseCase
import com.tradingexchange.app.ui.chart.InstrumentChartViewModel
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class InstrumentChartViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun liveQuoteInSameBucketUpdatesLinePoint() = runTest {
        val quotesRepository = FakeQuotesRepository()
        val viewModel = chartViewModel(quotesRepository)

        viewModel.load("SBER")
        advanceUntilIdle()
        quotesRepository.emit(Quote("SBER", BigDecimal("252.4200"), "RUB", 10_000L))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.linePoints.size)
        assertEquals(BigDecimal("252.4200"), state.linePoints.last().price)
    }

    @Test
    fun liveQuoteInNewBucketAppendsLinePoint() = runTest {
        val quotesRepository = FakeQuotesRepository()
        val viewModel = chartViewModel(quotesRepository)

        viewModel.load("SBER")
        advanceUntilIdle()
        quotesRepository.emit(Quote("SBER", BigDecimal("252.4200"), "RUB", 61_000L))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.linePoints.size)
        assertEquals(60_000L, state.linePoints.last().timestampMs)
        assertEquals(BigDecimal("252.4200"), state.linePoints.last().price)
    }

    @Test
    fun rangeChangeReplacesInvalidIntervalWithRecommendedInterval() = runTest {
        val viewModel = chartViewModel(FakeQuotesRepository())

        viewModel.load("SBER")
        advanceUntilIdle()
        viewModel.selectRange(ChartRange.MINUTE)
        advanceUntilIdle()

        assertEquals(ChartInterval.SECOND, viewModel.state.value.interval)
    }

    @Test
    fun liveQuoteUpdatesAndCreatesCandleBuckets() = runTest {
        val quotesRepository = FakeQuotesRepository()
        val viewModel = chartViewModel(quotesRepository)

        viewModel.load("SBER")
        advanceUntilIdle()
        viewModel.selectType(ChartType.CANDLES)
        advanceUntilIdle()
        quotesRepository.emit(Quote("SBER", BigDecimal("253.0000"), "RUB", 10_000L))
        advanceUntilIdle()
        quotesRepository.emit(Quote("SBER", BigDecimal("254.0000"), "RUB", 61_000L))
        advanceUntilIdle()

        val candles = viewModel.state.value.candles
        assertEquals(BigDecimal("253.0000"), candles.first().close)
        assertTrue(candles.any { it.timestampMs == 60_000L && it.close == BigDecimal("254.0000") })
    }

    private fun chartViewModel(quotesRepository: FakeQuotesRepository) = InstrumentChartViewModel(
        getLineChartUseCase = GetLineChartUseCase(FakeBrokerRepository()),
        getCandleChartUseCase = GetCandleChartUseCase(FakeBrokerRepository()),
        quotesRepository = quotesRepository,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeQuotesRepository : QuotesRepository {
    private val mutableQuotes = MutableStateFlow<Map<String, Quote>>(emptyMap())
    override val quotes: Flow<Map<String, Quote>> = mutableQuotes
    override val isConnected: Flow<Boolean> = MutableStateFlow(true)
    override fun connect() = Unit
    override fun disconnect() = Unit
    override fun subscribe(tickers: Set<String>) = Unit
    override fun unsubscribe(tickers: Set<String>) = Unit
    fun emit(quote: Quote) {
        mutableQuotes.value = mutableQuotes.value + (quote.ticker to quote)
    }
}

private class FakeBrokerRepository : BrokerRepository {
    override fun observePortfolio(): Flow<Portfolio?> = MutableStateFlow(null)
    override fun observeInstruments(): Flow<List<Instrument>> = MutableStateFlow(emptyList())
    override fun observeOrders(): Flow<List<Order>> = MutableStateFlow(emptyList())
    override fun observeTransactions(): Flow<List<Transaction>> = MutableStateFlow(emptyList())
    override suspend fun getProfile(): UserProfile = UserProfile("u", "e", "n", "CLIENT")
    override suspend fun refreshPortfolio(): Portfolio = Portfolio(emptyList(), Cash("RUB", BigDecimal.ZERO))
    override suspend fun searchInstruments(query: String): List<Instrument> = emptyList()
    override suspend fun getLineChart(ticker: String, range: ChartRange, interval: ChartInterval): LineChart =
        LineChart(ticker, "RUB", range.apiValue, interval.apiValue, listOf(LineChartPoint(0L, BigDecimal("251.0000"))))
    override suspend fun getCandleChart(ticker: String, range: ChartRange, interval: ChartInterval): CandleChart =
        CandleChart(ticker, "RUB", range.apiValue, interval.apiValue, listOf(Candle(0L, BigDecimal("251.0000"), BigDecimal("252.0000"), BigDecimal("250.0000"), BigDecimal("251.5000"))))
    override suspend fun createOrder(command: CreateOrderCommand): Order = error("not needed")
    override suspend fun refreshOrders(cursor: String?): ResultPage<Order> = ResultPage(emptyList(), null)
    override suspend fun refreshTransactions(cursor: String?): ResultPage<Transaction> = ResultPage(emptyList(), null)
    override suspend fun clearLocalState() = Unit
}
