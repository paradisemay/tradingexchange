package com.example.transactions

import com.example.domain.Transaction
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class TransactionRepository {

    fun findByUser(conn: Connection, userId: UUID, limit: Int, cursor: Instant?): List<Transaction> {
        val sql = if (cursor != null) {
            """SELECT id, user_id, order_id, ticker, type, amount, quantity, created_at
               FROM transactions WHERE user_id = ? AND created_at < ?
               ORDER BY created_at DESC LIMIT ?"""
        } else {
            """SELECT id, user_id, order_id, ticker, type, amount, quantity, created_at
               FROM transactions WHERE user_id = ?
               ORDER BY created_at DESC LIMIT ?"""
        }
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, userId)
            if (cursor != null) {
                stmt.setTimestamp(2, Timestamp.from(cursor))
                stmt.setInt(3, limit)
            } else {
                stmt.setInt(2, limit)
            }
            val rs = stmt.executeQuery()
            val result = mutableListOf<Transaction>()
            while (rs.next()) result.add(Transaction(
                id = UUID.fromString(rs.getString("id")),
                userId = UUID.fromString(rs.getString("user_id")),
                orderId = rs.getString("order_id")?.let { UUID.fromString(it) },
                ticker = rs.getString("ticker"),
                type = rs.getString("type"),
                amount = rs.getBigDecimal("amount"),
                quantity = rs.getBigDecimal("quantity"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
            ))
            return result
        }
    }
}
