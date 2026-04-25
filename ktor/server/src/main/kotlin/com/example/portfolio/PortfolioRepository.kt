package com.example.portfolio

import com.example.domain.Account
import com.example.domain.PortfolioPosition
import java.sql.Connection
import java.util.UUID

class PortfolioRepository {

    fun findAccount(conn: Connection, userId: UUID): Account? {
        val sql = """
            SELECT id, user_id, currency, cash_balance, reserved_balance
            FROM accounts WHERE user_id = ? AND currency = 'RUB'
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, userId)
            val rs = stmt.executeQuery()
            return if (rs.next()) Account(
                id = UUID.fromString(rs.getString("id")),
                userId = UUID.fromString(rs.getString("user_id")),
                currency = rs.getString("currency"),
                cashBalance = rs.getBigDecimal("cash_balance"),
                reservedBalance = rs.getBigDecimal("reserved_balance"),
            ) else null
        }
    }

    fun findPositions(conn: Connection, userId: UUID): List<PortfolioPosition> {
        val sql = """
            SELECT user_id, ticker, quantity, avg_price, updated_at
            FROM portfolio_positions WHERE user_id = ? AND quantity > 0
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, userId)
            val rs = stmt.executeQuery()
            val result = mutableListOf<PortfolioPosition>()
            while (rs.next()) result.add(PortfolioPosition(
                userId = UUID.fromString(rs.getString("user_id")),
                ticker = rs.getString("ticker"),
                quantity = rs.getBigDecimal("quantity"),
                avgPrice = rs.getBigDecimal("avg_price"),
                updatedAt = rs.getTimestamp("updated_at").toInstant(),
            ))
            return result
        }
    }
}
