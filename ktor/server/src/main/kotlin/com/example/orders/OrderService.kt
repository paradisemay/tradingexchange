package com.example.orders

import com.example.db.withTransaction
import com.example.domain.AppException
import com.example.domain.ErrorCode
import com.example.domain.Order
import com.example.instruments.InstrumentRepository
import com.example.quotes.PriceCache
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.StatusCode
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

@Serializable
data class CreateOrderRequest(
    val ticker: String,
    val side: String,
    val orderType: String,
    val quantity: String,
    val limitPrice: String? = null,
)

class OrderService(
    private val ds: DataSource,
    private val priceCache: PriceCache,
    private val orderRepo: OrderRepository = OrderRepository(),
    private val instrumentRepo: InstrumentRepository = InstrumentRepository(),
) {
    private val tracer by lazy { GlobalOpenTelemetry.get().getTracer("ktor-backend") }

    suspend fun createOrder(userId: UUID, req: CreateOrderRequest): Order {
        validate(req)
        val quantity = BigDecimal(req.quantity)
        val limitPrice = req.limitPrice?.let { BigDecimal(it) }

        val span = tracer.spanBuilder("order.execute")
            .setAttribute("order.side", req.side)
            .setAttribute("order.type", req.orderType)
            .setAttribute("order.ticker", req.ticker)
            .startSpan()
        val scope = span.makeCurrent()
        return try {
            ds.withTransaction { conn ->
                val instrument = instrumentRepo.findByTicker(conn, req.ticker)
                    ?: throw AppException(ErrorCode.INSTRUMENT_NOT_FOUND, "Instrument ${req.ticker} not found")

                val executionPrice = resolvePrice(instrument, req.orderType, limitPrice)

                when (req.side) {
                    "BUY" -> executeBuy(conn, userId, req.ticker, quantity, executionPrice)
                    "SELL" -> executeSell(conn, userId, req.ticker, quantity, executionPrice)
                    else -> throw AppException(ErrorCode.VALIDATION_ERROR, "Invalid side: ${req.side}")
                }
            }
        } catch (e: Exception) {
            span.setStatus(StatusCode.ERROR, e.message ?: "error")
            span.recordException(e)
            throw e
        } finally {
            scope.close()
            span.end()
        }
    }

    private fun resolvePrice(
        instrument: com.example.domain.Instrument,
        orderType: String,
        limitPrice: BigDecimal?,
    ): BigDecimal = when (orderType) {
        "LIMIT" -> limitPrice
            ?: throw AppException(ErrorCode.VALIDATION_ERROR, "limitPrice is required for LIMIT orders")
        "MARKET" -> priceCache.getPrice(instrument.ticker)
            ?: instrument.lastPrice
            ?: throw AppException(ErrorCode.QUOTE_UNAVAILABLE, "No price available for ${instrument.ticker}")
        else -> throw AppException(ErrorCode.VALIDATION_ERROR, "Invalid orderType: $orderType")
    }

    private fun executeBuy(conn: Connection, userId: UUID, ticker: String, quantity: BigDecimal, price: BigDecimal): Order {
        val total = price.multiply(quantity).setScale(4, RoundingMode.HALF_UP)

        // Порядок блокировок: accounts → portfolio_positions
        val accountRow = conn.prepareStatement(
            "SELECT id, cash_balance FROM accounts WHERE user_id = ? AND currency = 'RUB' FOR UPDATE"
        ).use { stmt ->
            stmt.setObject(1, userId)
            val rs = stmt.executeQuery()
            if (!rs.next()) throw AppException(ErrorCode.INTERNAL_ERROR, "Account not found")
            rs.getString("id") to rs.getBigDecimal("cash_balance")
        }
        val (accountId, cashBalance) = accountRow

        conn.prepareStatement(
            "SELECT quantity, avg_price FROM portfolio_positions WHERE user_id = ? AND ticker = ? FOR UPDATE"
        ).use { stmt ->
            stmt.setObject(1, userId)
            stmt.setString(2, ticker)
            stmt.executeQuery()
        }

        if (cashBalance < total) throw AppException(
            ErrorCode.INSUFFICIENT_FUNDS,
            "Insufficient funds",
            mapOf("required" to total.toPlainString(), "available" to cashBalance.toPlainString()),
        )

        val order = orderRepo.insert(conn, userId, ticker, "BUY", "MARKET", "FILLED", quantity, null, price)

        conn.prepareStatement("UPDATE accounts SET cash_balance = cash_balance - ? WHERE id = ?").use { stmt ->
            stmt.setBigDecimal(1, total)
            stmt.setObject(2, UUID.fromString(accountId))
            stmt.executeUpdate()
        }

        upsertPosition(conn, userId, ticker, quantity, price)

        insertTransaction(conn, userId, order.id, ticker, "BUY", total, quantity)
        return order
    }

    private fun executeSell(conn: Connection, userId: UUID, ticker: String, quantity: BigDecimal, price: BigDecimal): Order {
        val total = price.multiply(quantity).setScale(4, RoundingMode.HALF_UP)

        // Порядок блокировок: accounts → portfolio_positions
        conn.prepareStatement(
            "SELECT id FROM accounts WHERE user_id = ? AND currency = 'RUB' FOR UPDATE"
        ).use { stmt ->
            stmt.setObject(1, userId)
            stmt.executeQuery()
        }

        val posRow = conn.prepareStatement(
            "SELECT quantity, avg_price FROM portfolio_positions WHERE user_id = ? AND ticker = ? FOR UPDATE"
        ).use { stmt ->
            stmt.setObject(1, userId)
            stmt.setString(2, ticker)
            val rs = stmt.executeQuery()
            if (!rs.next()) throw AppException(ErrorCode.INSUFFICIENT_POSITION, "No position for $ticker")
            rs.getBigDecimal("quantity") to rs.getBigDecimal("avg_price")
        }
        val (currentQty) = posRow

        if (currentQty < quantity) throw AppException(
            ErrorCode.INSUFFICIENT_POSITION,
            "Insufficient position",
            mapOf("requested" to quantity.toPlainString(), "available" to currentQty.toPlainString()),
        )

        val newQty = currentQty.subtract(quantity)
        if (newQty.compareTo(BigDecimal.ZERO) == 0) {
            conn.prepareStatement("UPDATE portfolio_positions SET quantity = 0, avg_price = 0, updated_at = NOW() WHERE user_id = ? AND ticker = ?").use { stmt ->
                stmt.setObject(1, userId)
                stmt.setString(2, ticker)
                stmt.executeUpdate()
            }
        } else {
            conn.prepareStatement("UPDATE portfolio_positions SET quantity = quantity - ?, updated_at = NOW() WHERE user_id = ? AND ticker = ?").use { stmt ->
                stmt.setBigDecimal(1, quantity)
                stmt.setObject(2, userId)
                stmt.setString(3, ticker)
                stmt.executeUpdate()
            }
        }

        conn.prepareStatement("UPDATE accounts SET cash_balance = cash_balance + ? WHERE user_id = ? AND currency = 'RUB'").use { stmt ->
            stmt.setBigDecimal(1, total)
            stmt.setObject(2, userId)
            stmt.executeUpdate()
        }

        val order = orderRepo.insert(conn, userId, ticker, "SELL", "MARKET", "FILLED", quantity, null, price)
        insertTransaction(conn, userId, order.id, ticker, "SELL", total, quantity)
        return order
    }

    private fun upsertPosition(conn: Connection, userId: UUID, ticker: String, quantity: BigDecimal, price: BigDecimal) {
        val sql = """
            INSERT INTO portfolio_positions (user_id, ticker, quantity, avg_price, updated_at)
            VALUES (?, ?, ?, ?, NOW())
            ON CONFLICT (user_id, ticker) DO UPDATE SET
                avg_price = (portfolio_positions.avg_price * portfolio_positions.quantity + EXCLUDED.avg_price * EXCLUDED.quantity)
                            / (portfolio_positions.quantity + EXCLUDED.quantity),
                quantity  = portfolio_positions.quantity + EXCLUDED.quantity,
                updated_at = NOW()
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, userId)
            stmt.setString(2, ticker)
            stmt.setBigDecimal(3, quantity)
            stmt.setBigDecimal(4, price)
            stmt.executeUpdate()
        }
    }

    private fun insertTransaction(conn: Connection, userId: UUID, orderId: UUID, ticker: String, type: String, amount: BigDecimal, quantity: BigDecimal) {
        val sql = "INSERT INTO transactions (user_id, order_id, ticker, type, amount, quantity) VALUES (?, ?, ?, ?, ?, ?)"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, userId)
            stmt.setObject(2, orderId)
            stmt.setString(3, ticker)
            stmt.setString(4, type)
            stmt.setBigDecimal(5, amount)
            stmt.setBigDecimal(6, quantity)
            stmt.executeUpdate()
        }
    }

    private fun validate(req: CreateOrderRequest) {
        if (req.ticker.isBlank()) throw AppException(ErrorCode.VALIDATION_ERROR, "ticker is required")
        if (req.side !in listOf("BUY", "SELL")) throw AppException(ErrorCode.VALIDATION_ERROR, "side must be BUY or SELL")
        if (req.orderType !in listOf("MARKET", "LIMIT")) throw AppException(ErrorCode.VALIDATION_ERROR, "orderType must be MARKET or LIMIT")
        val qty = req.quantity.toBigDecimalOrNull()
            ?: throw AppException(ErrorCode.VALIDATION_ERROR, "quantity must be a valid number")
        if (qty <= BigDecimal.ZERO) throw AppException(ErrorCode.VALIDATION_ERROR, "quantity must be positive")
    }
}
