package com.tradingexchange.app.domain.usecase

import com.tradingexchange.app.domain.model.CreateOrderCommand
import com.tradingexchange.app.domain.model.ChartRange
import com.tradingexchange.app.domain.model.ChartInterval
import com.tradingexchange.app.domain.repository.AuthRepository
import com.tradingexchange.app.domain.repository.BrokerRepository
import com.tradingexchange.app.domain.repository.QuotesRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) = authRepository.login(email, password)
}

class RegisterUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, fullName: String) =
        authRepository.register(email, password, fullName)
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val brokerRepository: BrokerRepository,
    private val quotesRepository: QuotesRepository,
) {
    suspend operator fun invoke() {
        quotesRepository.disconnect()
        brokerRepository.clearLocalState()
        authRepository.logout()
    }
}

class RefreshPortfolioUseCase @Inject constructor(private val brokerRepository: BrokerRepository) {
    suspend operator fun invoke() = brokerRepository.refreshPortfolio()
}

class SearchInstrumentsUseCase @Inject constructor(private val brokerRepository: BrokerRepository) {
    suspend operator fun invoke(query: String) = brokerRepository.searchInstruments(query)
}

class GetLineChartUseCase @Inject constructor(private val brokerRepository: BrokerRepository) {
    suspend operator fun invoke(ticker: String, range: ChartRange, interval: ChartInterval) =
        brokerRepository.getLineChart(ticker, range, interval)
}

class GetCandleChartUseCase @Inject constructor(private val brokerRepository: BrokerRepository) {
    suspend operator fun invoke(ticker: String, range: ChartRange, interval: ChartInterval) =
        brokerRepository.getCandleChart(ticker, range, interval)
}

class CreateOrderUseCase @Inject constructor(private val brokerRepository: BrokerRepository) {
    suspend operator fun invoke(command: CreateOrderCommand) = brokerRepository.createOrder(command)
}
