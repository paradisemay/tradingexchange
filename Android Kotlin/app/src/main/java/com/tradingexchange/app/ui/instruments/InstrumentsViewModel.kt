package com.tradingexchange.app.ui.instruments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingexchange.app.data.repository.RepositoryException
import com.tradingexchange.app.domain.model.Instrument
import com.tradingexchange.app.domain.model.userMessage
import com.tradingexchange.app.domain.repository.BrokerRepository
import com.tradingexchange.app.domain.usecase.SearchInstrumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InstrumentsUiState(
    val query: String = "SBER",
    val isLoading: Boolean = false,
    val instruments: List<Instrument> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class InstrumentsViewModel @Inject constructor(
    brokerRepository: BrokerRepository,
    private val searchInstrumentsUseCase: SearchInstrumentsUseCase,
) : ViewModel() {
    private val query = MutableStateFlow("SBER")
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<InstrumentsUiState> = combine(
        query,
        loading,
        brokerRepository.observeInstruments(),
        error,
    ) { queryValue, loadingValue, instruments, errorValue ->
        InstrumentsUiState(queryValue, loadingValue, instruments, errorValue)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstrumentsUiState())

    init {
        search()
    }

    fun updateQuery(value: String) {
        query.value = value
    }

    fun search() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { searchInstrumentsUseCase(query.value) }
                .onFailure { error.value = (it as? RepositoryException)?.appError?.userMessage() ?: "Поиск не удался" }
            loading.value = false
        }
    }
}
