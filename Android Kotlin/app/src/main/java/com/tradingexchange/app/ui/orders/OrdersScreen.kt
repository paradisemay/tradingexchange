package com.tradingexchange.app.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tradingexchange.app.domain.model.OrderSide
import com.tradingexchange.app.domain.model.OrderType

@Composable
fun OrdersScreen(viewModel: OrdersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Заявки")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = state.side == OrderSide.BUY, onClick = { viewModel.updateSide(OrderSide.BUY) }, label = { Text("BUY") })
            FilterChip(selected = state.side == OrderSide.SELL, onClick = { viewModel.updateSide(OrderSide.SELL) }, label = { Text("SELL") })
            FilterChip(selected = state.orderType == OrderType.MARKET, onClick = { viewModel.updateOrderType(OrderType.MARKET) }, label = { Text("MARKET") })
            FilterChip(selected = state.orderType == OrderType.LIMIT, onClick = { viewModel.updateOrderType(OrderType.LIMIT) }, label = { Text("LIMIT") })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(state.ticker, viewModel::updateTicker, label = { Text("Тикер") }, modifier = Modifier.weight(1f))
            OutlinedTextField(state.quantity, viewModel::updateQuantity, label = { Text("Кол-во") }, modifier = Modifier.weight(1f))
        }
        if (state.orderType == OrderType.LIMIT) {
            OutlinedTextField(state.limitPrice, viewModel::updateLimitPrice, label = { Text("Limit price") }, modifier = Modifier.fillMaxWidth())
        }
        Button(onClick = viewModel::create, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) { Text("Создать заявку") }
        state.message?.let { Text(it) }
        if (state.isLoading) CircularProgressIndicator()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.orders) { order ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${order.side} ${order.ticker} ${order.quantity.toPlainString()}")
                        Text("${order.orderType} / ${order.status}")
                        Text("Цена: ${order.executedPrice?.toPlainString() ?: "-"}")
                        Text(order.createdAt)
                    }
                }
            }
            item {
                state.nextCursor?.let { Button(onClick = { viewModel.load(it) }) { Text("Еще") } }
            }
        }
    }
}
