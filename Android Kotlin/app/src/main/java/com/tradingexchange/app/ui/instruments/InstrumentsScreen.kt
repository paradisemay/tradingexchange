package com.tradingexchange.app.ui.instruments

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun InstrumentsScreen(
    onOpenInstrument: (String) -> Unit,
    viewModel: InstrumentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Instruments", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                label = { Text("Ticker or name") },
                modifier = Modifier.weight(1f),
            )
            Button(onClick = viewModel::search) { Text("Find") }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.isLoading) CircularProgressIndicator()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.instruments) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${item.ticker} - ${item.name}", style = MaterialTheme.typography.titleMedium)
                        Text("Price: ${item.lastPrice?.toPlainString() ?: "-"} ${item.currency}")
                        Text("Lot: ${item.lotSize}")
                        Button(onClick = { onOpenInstrument(item.ticker) }) {
                            Text("Open chart")
                        }
                    }
                }
            }
        }
    }
}
