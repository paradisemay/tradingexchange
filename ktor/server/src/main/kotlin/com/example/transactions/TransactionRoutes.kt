package com.example.transactions

import com.example.db.withConnection
import com.example.portfolio.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import java.time.Instant
import javax.sql.DataSource

@Serializable
data class TransactionResponse(
    val id: String,
    val type: String,
    val ticker: String?,
    val amount: String,
    val quantity: String?,
    val createdAt: String,
)

@Serializable
data class TransactionListResponse(val transactions: List<TransactionResponse>, val nextCursor: String?)

fun Route.transactionRoutes(ds: DataSource, repo: TransactionRepository = TransactionRepository()) {
    authenticate("auth-jwt") {
        get("/api/v1/transactions") {
            val userId = call.userId()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
            val cursor = call.request.queryParameters["cursor"]?.let { runCatching { Instant.parse(it) }.getOrNull() }

            val txs = ds.withConnection { conn -> repo.findByUser(conn, userId, limit, cursor) }
            val nextCursor = if (txs.size == limit) txs.last().createdAt.toString() else null
            call.respond(HttpStatusCode.OK, TransactionListResponse(txs.map { it.toResponse() }, nextCursor))
        }
    }
}

private fun com.example.domain.Transaction.toResponse() = TransactionResponse(
    id = id.toString(),
    type = type,
    ticker = ticker,
    amount = amount.toPlainString(),
    quantity = quantity?.toPlainString(),
    createdAt = createdAt.toString(),
)
