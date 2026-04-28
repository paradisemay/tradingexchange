package com.tradingexchange.app.ui.instruments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingexchange.app.data.repository.RepositoryException
import com.tradingexchange.app.domain.model.Instrument
import com.tradingexchange.app.domain.model.Quote
import com.tradingexchange.app.domain.model.userMessage
import com.tradingexchange.app.domain.repository.QuotesRepository
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
    val query: String = "",
    val isLoading: Boolean = false,
    val instruments: List<Instrument> = emptyList(),
    val quotes: Map<String, Quote> = emptyMap(),
    val error: String? = null,
)

@HiltViewModel
class InstrumentsViewModel @Inject constructor(
    private val searchInstrumentsUseCase: SearchInstrumentsUseCase,
    private val quotesRepository: QuotesRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(false)
    private val visibleInstruments = MutableStateFlow<List<Instrument>>(emptyList())
    private val error = MutableStateFlow<String?>(null)
    private var subscribedTickers: Set<String> = emptySet()

    val state: StateFlow<InstrumentsUiState> = combine(
        query,
        loading,
        visibleInstruments,
        quotesRepository.quotes,
        error,
    ) { queryValue, loadingValue, instruments, quotes, errorValue ->
        InstrumentsUiState(queryValue, loadingValue, instruments, quotes, errorValue)
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
                .onSuccess { instruments ->
                    visibleInstruments.value = instruments
                    subscribeVisible(instruments)
                }
                .onFailure { throwable ->
                    error.value = (throwable as? RepositoryException)?.appError?.userMessage() ?: "Search failed"
                }
            loading.value = false
        }
    }

    override fun onCleared() {
        if (subscribedTickers.isNotEmpty()) quotesRepository.unsubscribe(subscribedTickers)
        super.onCleared()
    }

    private fun subscribeVisible(instruments: List<Instrument>) {
        val next = instruments.map { it.ticker.uppercase() }.toSet()
        val removed = subscribedTickers - next
        val added = next - subscribedTickers
        if (removed.isNotEmpty()) quotesRepository.unsubscribe(removed)
        if (added.isNotEmpty()) {
            quotesRepository.connect()
            quotesRepository.subscribe(added)
        }
        subscribedTickers = next
    }
}
