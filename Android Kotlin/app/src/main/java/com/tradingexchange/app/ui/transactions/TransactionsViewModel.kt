package com.tradingexchange.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingexchange.app.data.repository.RepositoryException
import com.tradingexchange.app.domain.model.Transaction
import com.tradingexchange.app.domain.model.userMessage
import com.tradingexchange.app.domain.repository.BrokerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(private val brokerRepository: BrokerRepository) : ViewModel() {
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val nextCursor = MutableStateFlow<String?>(null)

    val state: StateFlow<TransactionsUiState> = combine(
        brokerRepository.observeTransactions(),
        loading,
        nextCursor,
        error,
    ) { transactions, loadingValue, cursorValue, errorValue ->
        TransactionsUiState(loadingValue, transactions, cursorValue, errorValue)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    init {
        load()
    }

    fun load(cursor: String? = null) {
        viewModelScope.launch {
            loading.value = true
            runCatching { brokerRepository.refreshTransactions(cursor) }
                .onSuccess { nextCursor.value = it.nextCursor }
                .onFailure { error.value = (it as? RepositoryException)?.appError?.userMessage() ?: "Не удалось загрузить операции" }
            loading.value = false
        }
    }
}
