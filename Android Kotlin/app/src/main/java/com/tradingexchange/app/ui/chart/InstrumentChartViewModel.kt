package com.tradingexchange.app.ui.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingexchange.app.data.repository.RepositoryException
import com.tradingexchange.app.domain.model.Candle
import com.tradingexchange.app.domain.model.ChartInterval
import com.tradingexchange.app.domain.model.ChartRange
import com.tradingexchange.app.domain.model.ChartType
import com.tradingexchange.app.domain.model.LineChartPoint
import com.tradingexchange.app.domain.model.Quote
import com.tradingexchange.app.domain.repository.QuotesRepository
import com.tradingexchange.app.domain.model.isValidFor
import com.tradingexchange.app.domain.model.recommendedInterval
import com.tradingexchange.app.domain.model.toMillis
import com.tradingexchange.app.domain.model.userMessage
import com.tradingexchange.app.domain.usecase.GetCandleChartUseCase
import com.tradingexchange.app.domain.usecase.GetLineChartUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InstrumentChartUiState(
    val ticker: String = "",
    val chartType: ChartType = ChartType.LINE,
    val range: ChartRange = ChartRange.DAY,
    val interval: ChartInterval = ChartInterval.MINUTE,
    val isLoading: Boolean = false,
    val currency: String = "RUB",
    val linePoints: List<LineChartPoint> = emptyList(),
    val candles: List<Candle> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class InstrumentChartViewModel @Inject constructor(
    private val getLineChartUseCase: GetLineChartUseCase,
    private val getCandleChartUseCase: GetCandleChartUseCase,
    private val quotesRepository: QuotesRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(InstrumentChartUiState())
    val state: StateFlow<InstrumentChartUiState> = mutableState
    private var subscribedTicker: String? = null

    init {
        viewModelScope.launch {
            quotesRepository.quotes.collect { quotes ->
                val ticker = mutableState.value.ticker
                val quote = quotes[ticker] ?: return@collect
                applyLiveQuote(quote)
            }
        }
    }

    fun load(ticker: String) {
        if (ticker.isBlank()) return
        val normalized = ticker.uppercase()
        if (mutableState.value.ticker == normalized && hasChartData()) return
        subscribedTicker?.takeIf { it != normalized }?.let { quotesRepository.unsubscribe(setOf(it)) }
        subscribedTicker = normalized
        quotesRepository.connect()
        quotesRepository.subscribe(setOf(normalized))
        mutableState.update { it.copy(ticker = normalized) }
        refresh()
    }

    fun selectType(type: ChartType) {
        mutableState.update { it.copy(chartType = type) }
        refresh()
    }

    fun selectRange(range: ChartRange) {
        mutableState.update { current ->
            val interval = current.interval.takeIf { it.isValidFor(range) } ?: range.recommendedInterval()
            current.copy(range = range, interval = interval)
        }
        refresh()
    }

    fun selectInterval(interval: ChartInterval) {
        if (!interval.isValidFor(mutableState.value.range)) return
        mutableState.update { it.copy(interval = interval) }
        refresh()
    }

    fun refresh() {
        val snapshot = mutableState.value
        if (snapshot.ticker.isBlank()) return
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                if (snapshot.chartType == ChartType.LINE) {
                    val chart = getLineChartUseCase(snapshot.ticker, snapshot.range, snapshot.interval)
                    mutableState.update { it.copy(currency = chart.currency, linePoints = chart.points, candles = emptyList()) }
                } else {
                    val chart = getCandleChartUseCase(snapshot.ticker, snapshot.range, snapshot.interval)
                    mutableState.update { it.copy(currency = chart.currency, candles = chart.candles, linePoints = emptyList()) }
                }
            }.onFailure { throwable ->
                val message = (throwable as? RepositoryException)?.appError?.userMessage() ?: "Chart data is unavailable"
                mutableState.update { it.copy(error = message) }
            }
            mutableState.update { it.copy(isLoading = false) }
        }
    }

    override fun onCleared() {
        subscribedTicker?.let { quotesRepository.unsubscribe(setOf(it)) }
        super.onCleared()
    }

    private fun hasChartData(): Boolean =
        mutableState.value.linePoints.isNotEmpty() || mutableState.value.candles.isNotEmpty()

    private fun applyLiveQuote(quote: Quote) {
        mutableState.update { current ->
            if (quote.ticker != current.ticker) return@update current
            when (current.chartType) {
                ChartType.LINE -> current.copy(
                    currency = quote.currency,
                    linePoints = appendQuoteToLinePoints(current.linePoints, quote, current.range, current.interval),
                )
                ChartType.CANDLES -> current.copy(
                    currency = quote.currency,
                    candles = appendQuoteToCandles(current.candles, quote, current.range, current.interval),
                )
            }
        }
    }

    private fun appendQuoteToCandles(candles: List<Candle>, quote: Quote, range: ChartRange, interval: ChartInterval): List<Candle> {
        val bucket = quote.timestampMs.floorToInterval(interval.toMillis())
        val last = candles.lastOrNull()
        if (last == null || last.timestampMs != bucket) {
            return (candles + Candle(bucket, quote.price, quote.price, quote.price, quote.price))
                .trimCandlesToRangeWindow(bucket, range.toMillis(), pointLimit(range, interval))
        }
        val updated = last.copy(
            high = maxOf(last.high, quote.price),
            low = minOf(last.low, quote.price),
            close = quote.price,
        )
        return (candles.dropLast(1) + updated).trimCandlesToRangeWindow(bucket, range.toMillis(), pointLimit(range, interval))
    }

    private fun appendQuoteToLinePoints(points: List<LineChartPoint>, quote: Quote, range: ChartRange, interval: ChartInterval): List<LineChartPoint> {
        val bucket = quote.timestampMs.floorToInterval(interval.toMillis())
        val last = points.lastOrNull()
        val updated = if (last == null || last.timestampMs != bucket) {
            points + LineChartPoint(bucket, quote.price)
        } else {
            points.dropLast(1) + last.copy(price = quote.price)
        }
        return updated.trimLinePointsToRangeWindow(bucket, range.toMillis(), pointLimit(range, interval))
    }

    private fun Long.floorToInterval(intervalMs: Long): Long = (this / intervalMs) * intervalMs

    private fun List<LineChartPoint>.trimLinePointsToRangeWindow(latestBucket: Long, rangeMs: Long, limit: Int): List<LineChartPoint> {
        val minTimestamp = latestBucket - rangeMs
        return filter { it.timestampMs > minTimestamp }.takeLast(limit)
    }

    private fun List<Candle>.trimCandlesToRangeWindow(latestBucket: Long, rangeMs: Long, limit: Int): List<Candle> {
        val minTimestamp = latestBucket - rangeMs
        return filter { it.timestampMs > minTimestamp }.takeLast(limit)
    }

    private fun pointLimit(range: ChartRange, interval: ChartInterval): Int =
        ((range.toMillis() / interval.toMillis()).coerceAtLeast(1)).coerceAtMost(MAX_CHART_POINTS.toLong()).toInt()

    private companion object {
        const val MAX_CHART_POINTS = 160
    }
}
