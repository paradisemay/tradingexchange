package com.tradingexchange.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BrokerDao {
    @Query("SELECT * FROM portfolio_positions ORDER BY ticker")
    fun observePositions(): Flow<List<PortfolioPositionEntity>>

    @Query("SELECT * FROM cash WHERE currency = 'RUB' LIMIT 1")
    fun observeCash(): Flow<CashEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replacePositions(items: List<PortfolioPositionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceCash(item: CashEntity)

    @Query("DELETE FROM portfolio_positions")
    suspend fun clearPositions()

    @Query("SELECT * FROM instruments ORDER BY ticker")
    fun observeInstruments(): Flow<List<InstrumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInstruments(items: List<InstrumentEntity>)

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrders(items: List<OrderEntity>)

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(items: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuote(item: QuoteEntity)

    @Query("DELETE FROM portfolio_positions")
    suspend fun clearPortfolio()

    @Query("DELETE FROM instruments")
    suspend fun clearInstruments()

    @Query("DELETE FROM orders")
    suspend fun clearOrders()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM quotes")
    suspend fun clearQuotes()
}
