package com.tradingexchange.app.ui.portfolio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PortfolioScreen(
    onOpenInstrument: (String) -> Unit,
    viewModel: PortfolioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Портфель")
            Button(onClick = viewModel::refresh) { Text("Обновить") }
        }
        if (state.isOffline) Text("Котировки: reconnect / offline")
        state.error?.let { Text(it) }
        if (state.isLoading) CircularProgressIndicator()
        val portfolio = state.portfolio
        if (portfolio == null) {
            Spacer(Modifier.height(16.dp))
            Text("Данных пока нет")
        } else {
            Text("Деньги: ${portfolio.cash.available.toPlainString()} ${portfolio.cash.currency}")
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(portfolio.positions) { position ->
                    val live = state.quotes[position.ticker]?.price ?: position.currentPrice
                    Card(onClick = { onOpenInstrument(position.ticker) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(position.ticker)
                                Text("Кол-во: ${position.quantity.toPlainString()}")
                                Text("Средняя: ${position.avgPrice.toPlainString()}")
                                Text("Цена: ${live?.toPlainString() ?: "-"}")
                            }
                            Switch(checked = state.quotes.containsKey(position.ticker), onCheckedChange = { viewModel.toggleQuote(position.ticker, it) })
                        }
                    }
                }
            }
        }
    }
}
