package com.example.orders

import com.example.domain.Order
import java.math.BigDecimal
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class OrderRepository {

    fun insert(
        conn: Connection,
        userId: UUID,
        ticker: String,
        side: String,
        orderType: String,
        status: String,
        quantity: BigDecimal,
        price: BigDecimal?,
        executedPrice: BigDecimal?,
    ): Order {
        val sql = """
            INSERT INTO orders (user_id, ticker, side, order_type, status, quantity, price, executed_price)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, user_id, ticker, side, order_type, status, quantity, price, executed_price, created_at, updated_at
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, userId)
            stmt.setString(2, ticker)
            stmt.setString(3, side)
            stmt.setString(4, orderType)
            stmt.setString(5, status)
            stmt.setBigDecimal(6, quantity)
            stmt.setBigDecimal(7, price)
            stmt.setBigDecimal(8, executedPrice)
            val rs = stmt.executeQuery()
            if (rs.next()) return rs.toOrder()
            throw IllegalStateException("Order insert returned no rows")
        }
    }

    fun findByUser(conn: Connection, userId: UUID, limit: Int, cursor: Instant?): List<Order> {
        val sql = if (cursor != null) {
            "SELECT id, user_id, ticker, side, order_type, status, quantity, price, executed_price, created_at, updated_at FROM orders WHERE user_id = ? AND created_at < ? ORDER BY created_at DESC LIMIT ?"
        } else {
            "SELECT id, user_id, ticker, side, order_type, status, quantity, price, executed_price, created_at, updated_at FROM orders WHERE user_id = ? ORDER BY created_at DESC LIMIT ?"
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
            val result = mutableListOf<Order>()
            while (rs.next()) result.add(rs.toOrder())
            return result
        }
    }

    private fun java.sql.ResultSet.toOrder() = Order(
        id = UUID.fromString(getString("id")),
        userId = UUID.fromString(getString("user_id")),
        ticker = getString("ticker"),
        side = getString("side"),
        orderType = getString("order_type"),
        status = getString("status"),
        quantity = getBigDecimal("quantity"),
        price = getBigDecimal("price"),
        executedPrice = getBigDecimal("executed_price"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}
