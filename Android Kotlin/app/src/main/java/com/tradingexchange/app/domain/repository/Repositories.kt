package com.tradingexchange.app.domain.repository

import com.tradingexchange.app.domain.model.CreateOrderCommand
import com.tradingexchange.app.domain.model.CandleChart
import com.tradingexchange.app.domain.model.ChartInterval
import com.tradingexchange.app.domain.model.ChartRange
import com.tradingexchange.app.domain.model.Instrument
import com.tradingexchange.app.domain.model.LineChart
import com.tradingexchange.app.domain.model.Order
import com.tradingexchange.app.domain.model.Portfolio
import com.tradingexchange.app.domain.model.Quote
import com.tradingexchange.app.domain.model.ResultPage
import com.tradingexchange.app.domain.model.TokenPair
import com.tradingexchange.app.domain.model.Transaction
import com.tradingexchange.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    suspend fun register(email: String, password: String, fullName: String): TokenPair
    suspend fun login(email: String, password: String): TokenPair
    suspend fun logout()
}

interface BrokerRepository {
    fun observePortfolio(): Flow<Portfolio?>
    fun observeInstruments(): Flow<List<Instrument>>
    fun observeOrders(): Flow<List<Order>>
    fun observeTransactions(): Flow<List<Transaction>>
    suspend fun getProfile(): UserProfile
    suspend fun refreshPortfolio(): Portfolio
    suspend fun searchInstruments(query: String): List<Instrument>
    suspend fun getLineChart(ticker: String, range: ChartRange, interval: ChartInterval): LineChart
    suspend fun getCandleChart(ticker: String, range: ChartRange, interval: ChartInterval): CandleChart
    suspend fun createOrder(command: CreateOrderCommand): Order
    suspend fun refreshOrders(cursor: String? = null): ResultPage<Order>
    suspend fun refreshTransactions(cursor: String? = null): ResultPage<Transaction>
    suspend fun clearLocalState()
}

interface QuotesRepository {
    val quotes: Flow<Map<String, Quote>>
    val isConnected: Flow<Boolean>
    fun connect()
    fun disconnect()
    fun subscribe(tickers: Set<String>)
    fun unsubscribe(tickers: Set<String>)
}
