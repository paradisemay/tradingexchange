package com.tradingexchange.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PortfolioPositionEntity::class,
        CashEntity::class,
        InstrumentEntity::class,
        OrderEntity::class,
        TransactionEntity::class,
        QuoteEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun brokerDao(): BrokerDao
}
