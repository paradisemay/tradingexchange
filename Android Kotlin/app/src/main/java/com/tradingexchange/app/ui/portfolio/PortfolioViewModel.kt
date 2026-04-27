package com.tradingexchange.app.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tradingexchange.app.data.repository.RepositoryException
import com.tradingexchange.app.domain.model.Portfolio
import com.tradingexchange.app.domain.model.Quote
import com.tradingexchange.app.domain.model.userMessage
import com.tradingexchange.app.domain.repository.BrokerRepository
import com.tradingexchange.app.domain.repository.QuotesRepository
import com.tradingexchange.app.domain.usecase.RefreshPortfolioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PortfolioUiState(
    val isLoading: Boolean = false,
    val portfolio: Portfolio? = null,
    val quotes: Map<String, Quote> = emptyMap(),
    val isOffline: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    brokerRepository: BrokerRepository,
    private val quotesRepository: QuotesRepository,
    private val refreshPortfolioUseCase: RefreshPortfolioUseCase,
) : ViewModel() {
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<PortfolioUiState> = combine(
        brokerRepository.observePortfolio(),
        quotesRepository.quotes,
        quotesRepository.isConnected,
        loading,
        error,
    ) { portfolio, quotes, connected, loadingValue, errorValue ->
        PortfolioUiState(
            isLoading = loadingValue,
            portfolio = portfolio,
            quotes = quotes,
            isOffline = !connected,
            error = errorValue,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PortfolioUiState())

    init {
        quotesRepository.connect()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            runCatching { refreshPortfolioUseCase() }
                .onFailure { error.value = (it as? RepositoryException)?.appError?.userMessage() ?: "Не удалось загрузить портфель" }
            loading.value = false
            val tickers = state.value.portfolio?.positions?.map { it.ticker }?.toSet().orEmpty()
            if (tickers.isNotEmpty()) quotesRepository.subscribe(tickers)
        }
    }

    fun toggleQuote(ticker: String, enabled: Boolean) {
        if (enabled) quotesRepository.subscribe(setOf(ticker)) else quotesRepository.unsubscribe(setOf(ticker))
    }
}
