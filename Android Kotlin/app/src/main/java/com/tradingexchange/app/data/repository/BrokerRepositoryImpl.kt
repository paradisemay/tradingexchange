package com.tradingexchange.app.data.repository

import com.tradingexchange.app.data.local.BrokerDao
import com.tradingexchange.app.data.local.toDomain
import com.tradingexchange.app.data.local.toEntities
import com.tradingexchange.app.data.local.toEntity
import com.tradingexchange.app.data.remote.BrokerApi
import com.tradingexchange.app.data.remote.RemoteErrorMapper
import com.tradingexchange.app.data.remote.toDomain
import com.tradingexchange.app.data.remote.toDto
import com.tradingexchange.app.domain.model.Cash
import com.tradingexchange.app.domain.model.CreateOrderCommand
import com.tradingexchange.app.domain.model.Instrument
import com.tradingexchange.app.domain.model.Order
import com.tradingexchange.app.domain.model.Portfolio
import com.tradingexchange.app.domain.model.ResultPage
import com.tradingexchange.app.domain.model.Transaction
import com.tradingexchange.app.domain.model.UserProfile
import com.tradingexchange.app.domain.repository.BrokerRepository
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class BrokerRepositoryImpl @Inject constructor(
    private val api: BrokerApi,
    private val dao: BrokerDao,
    private val errorMapper: RemoteErrorMapper,
) : BrokerRepository {
    override fun observePortfolio(): Flow<Portfolio?> =
        combine(dao.observePositions(), dao.observeCash()) { positions, cash ->
            val safeCash = cash?.toDomain() ?: Cash("RUB", BigDecimal.ZERO)
            Portfolio(positions.map { it.toDomain() }, safeCash)
        }

    override fun observeInstruments(): Flow<List<Instrument>> =
        dao.observeInstruments().map { entities -> entities.map { it.toDomain() } }

    override fun observeOrders(): Flow<List<Order>> =
        dao.observeOrders().map { entities -> entities.map { it.toDomain() } }

    override fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeTransactions().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getProfile(): UserProfile = wrap { api.me().toDomain() }

    override suspend fun refreshPortfolio(): Portfolio = wrap {
        val portfolio = api.portfolio().toDomain()
        val (positions, cash) = portfolio.toEntities()
        dao.clearPositions()
        dao.replacePositions(positions)
        dao.replaceCash(cash)
        portfolio
    }

    override suspend fun searchInstruments(query: String): List<Instrument> = wrap {
        val instruments = api.instruments(query.ifBlank { null }).map { it.toDomain() }
        dao.upsertInstruments(instruments.map { it.toEntity() })
        instruments
    }

    override suspend fun createOrder(command: CreateOrderCommand): Order = wrap {
        val order = api.createOrder(command.toDto()).toDomain()
        dao.upsertOrders(listOf(order.toEntity()))
        refreshPortfolio()
        order
    }

    override suspend fun refreshOrders(cursor: String?): ResultPage<Order> = wrap {
        val page = api.orders(cursor = cursor)
        val orders = page.orders.map { it.toDomain() }
        dao.upsertOrders(orders.map { it.toEntity() })
        ResultPage(orders, page.nextCursor)
    }

    override suspend fun refreshTransactions(cursor: String?): ResultPage<Transaction> = wrap {
        val page = api.transactions(cursor = cursor)
        val transactions = page.transactions.map { it.toDomain() }
        dao.upsertTransactions(transactions.map { it.toEntity() })
        ResultPage(transactions, page.nextCursor)
    }

    override suspend fun clearLocalState() {
        dao.clearPortfolio()
        dao.clearInstruments()
        dao.clearOrders()
        dao.clearTransactions()
        dao.clearQuotes()
    }

    private suspend fun <T> wrap(block: suspend () -> T): T =
        runCatching { block() }.getOrElse { throw RepositoryException(errorMapper.map(it), it) }
}

class RepositoryException(val appError: com.tradingexchange.app.domain.model.AppError, cause: Throwable) : RuntimeException(cause)
