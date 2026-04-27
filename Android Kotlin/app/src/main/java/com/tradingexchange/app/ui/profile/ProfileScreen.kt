package com.tradingexchange.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Профиль")
        if (state.isLoading) CircularProgressIndicator()
        state.profile?.let {
            Text(it.email)
            Text(it.fullName ?: "Без имени")
            Text("Роль: ${it.role}")
        }
        state.error?.let { Text(it) }
        Button(onClick = viewModel::load, modifier = Modifier.fillMaxWidth()) { Text("Обновить") }
        OutlinedButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) { Text("Выйти") }
    }
}
