package com.example.instruments

import com.example.domain.Instrument
import java.math.BigDecimal
import java.sql.Connection

class InstrumentRepository {

    fun findAll(conn: Connection, query: String?): List<Instrument> {
        val sql = if (query.isNullOrBlank()) {
            "SELECT ticker, name, currency, lot_size, is_active, last_price FROM instruments WHERE is_active = TRUE ORDER BY ticker"
        } else {
            "SELECT ticker, name, currency, lot_size, is_active, last_price FROM instruments WHERE is_active = TRUE AND (ticker ILIKE ? OR name ILIKE ?) ORDER BY ticker LIMIT 50"
        }
        conn.prepareStatement(sql).use { stmt ->
            if (!query.isNullOrBlank()) {
                val pattern = "%$query%"
                stmt.setString(1, pattern)
                stmt.setString(2, pattern)
            }
            val rs = stmt.executeQuery()
            val result = mutableListOf<Instrument>()
            while (rs.next()) result.add(rs.toInstrument())
            return result
        }
    }

    fun findByTicker(conn: Connection, ticker: String): Instrument? {
        val sql = "SELECT ticker, name, currency, lot_size, is_active, last_price FROM instruments WHERE ticker = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, ticker)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.toInstrument() else null
        }
    }

    fun updateLastPrice(conn: Connection, ticker: String, price: BigDecimal) {
        val sql = "UPDATE instruments SET last_price = ? WHERE ticker = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setBigDecimal(1, price)
            stmt.setString(2, ticker)
            stmt.executeUpdate()
        }
    }

    private fun java.sql.ResultSet.toInstrument() = Instrument(
        ticker = getString("ticker"),
        name = getString("name"),
        currency = getString("currency"),
        lotSize = getInt("lot_size"),
        isActive = getBoolean("is_active"),
        lastPrice = getBigDecimal("last_price"),
    )
}
