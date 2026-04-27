package com.tradingexchange.app.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradingexchange.app.domain.model.Candle
import com.tradingexchange.app.domain.model.ChartInterval
import com.tradingexchange.app.domain.model.ChartRange
import com.tradingexchange.app.domain.model.ChartType
import com.tradingexchange.app.domain.model.LineChartPoint
import com.tradingexchange.app.domain.model.isValidFor
import java.math.BigDecimal

@Composable
fun InstrumentChartScreen(
    ticker: String,
    onBack: () -> Unit,
    viewModel: InstrumentChartViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(ticker) { viewModel.load(ticker) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBack) { Text("Back") }
            Text("${state.ticker} chart", style = MaterialTheme.typography.titleLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.chartType == ChartType.LINE,
                onClick = { viewModel.selectType(ChartType.LINE) },
                label = { Text("Line") },
            )
            FilterChip(
                selected = state.chartType == ChartType.CANDLES,
                onClick = { viewModel.selectType(ChartType.CANDLES) },
                label = { Text("Candles") },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartRange.entries.forEach { range ->
                AssistChip(
                    onClick = { viewModel.selectRange(range) },
                    label = { Text(range.label) },
                    enabled = state.range != range,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartInterval.entries.filter { it.isValidFor(state.range) }.forEach { interval ->
                AssistChip(
                    onClick = { viewModel.selectInterval(interval) },
                    label = { Text(interval.label) },
                    enabled = state.interval != interval,
                )
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.isLoading) CircularProgressIndicator()

        if (state.chartType == ChartType.LINE) {
            LineChartCanvas(state.linePoints, Modifier.fillMaxWidth().height(300.dp))
        } else {
            CandleChartCanvas(state.candles, Modifier.fillMaxWidth().height(300.dp))
        }
        Text(
            text = "Last ${latestPriceText(state)} ${state.currency}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text("Range ${state.range.label} / Interval ${state.interval.label} / points ${pointCountText(state)}")
    }
}

private fun latestPriceText(state: InstrumentChartUiState): String =
    when (state.chartType) {
        ChartType.LINE -> state.linePoints.lastOrNull()?.price?.toPlainString()
        ChartType.CANDLES -> state.candles.lastOrNull()?.close?.toPlainString()
    } ?: "-"

private fun pointCountText(state: InstrumentChartUiState): String =
    when (state.chartType) {
        ChartType.LINE -> state.linePoints.size
        ChartType.CANDLES -> state.candles.size
    }.toString()

@Composable
private fun LineChartCanvas(points: List<LineChartPoint>, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    ChartFrame(modifier) {
        if (points.size < 2) return@ChartFrame
        val values = points.map { it.price }
        val min = values.minOrNull() ?: BigDecimal.ZERO
        val max = values.maxOrNull() ?: BigDecimal.ONE
        val span = (max - min).takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
        val stepX = size.width / points.lastIndex.coerceAtLeast(1)
        var previous: Offset? = null
        points.forEachIndexed { index, point ->
            val normalized = ((point.price - min).toFloat() / span.toFloat()).coerceIn(0f, 1f)
            val current = Offset(index * stepX, size.height - normalized * size.height)
            previous?.let { drawLine(color, it, current, strokeWidth = 4f, cap = StrokeCap.Round) }
            previous = current
        }
    }
}

@Composable
private fun CandleChartCanvas(candles: List<Candle>, modifier: Modifier) {
    val positive = Color(0xFF0F766E)
    val negative = Color(0xFFB91C1C)
    ChartFrame(modifier) {
        if (candles.isEmpty()) return@ChartFrame
        val min = candles.minOf { it.low }
        val max = candles.maxOf { it.high }
        val span = (max - min).takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
        val slot = size.width / candles.size
        val bodyWidth = (slot * 0.55f).coerceAtLeast(3f)

        fun y(value: BigDecimal): Float {
            val normalized = ((value - min).toFloat() / span.toFloat()).coerceIn(0f, 1f)
            return size.height - normalized * size.height
        }

        candles.forEachIndexed { index, candle ->
            val centerX = index * slot + slot / 2
            val color = if (candle.close >= candle.open) positive else negative
            val openY = y(candle.open)
            val closeY = y(candle.close)
            drawLine(color, Offset(centerX, y(candle.high)), Offset(centerX, y(candle.low)), strokeWidth = 2f)
            drawRect(
                color = color,
                topLeft = Offset(centerX - bodyWidth / 2, minOf(openY, closeY)),
                size = Size(bodyWidth, kotlin.math.abs(closeY - openY).coerceAtLeast(2f)),
            )
        }
    }
}

@Composable
private fun ChartFrame(modifier: Modifier, content: DrawScope.() -> Unit) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        drawRect(grid, style = Stroke(width = 1f))
        val rowHeight = size.height / 4
        repeat(3) { index ->
            val y = rowHeight * (index + 1)
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        content()
    }
}

