package com.example.orders

import com.example.db.withConnection
import com.example.portfolio.userId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import java.time.Instant
import javax.sql.DataSource

@Serializable
data class OrderResponse(
    val orderId: String,
    val ticker: String,
    val side: String,
    val orderType: String,
    val status: String,
    val quantity: String,
    val executedPrice: String?,
    val createdAt: String,
)

@Serializable
data class OrderListResponse(val orders: List<OrderResponse>, val nextCursor: String?)

fun Route.orderRoutes(ds: DataSource, orderService: OrderService, repo: OrderRepository = OrderRepository()) {
    authenticate("auth-jwt") {
        post("/api/v1/orders") {
            val userId = call.userId()
            val req = call.receive<CreateOrderRequest>()
            val order = orderService.createOrder(userId, req)
            call.respond(HttpStatusCode.Created, order.toResponse())
        }

        get("/api/v1/orders") {
            val userId = call.userId()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
            val cursor = call.request.queryParameters["cursor"]?.let { runCatching { Instant.parse(it) }.getOrNull() }

            val orders = ds.withConnection { conn -> repo.findByUser(conn, userId, limit, cursor) }
            val nextCursor = if (orders.size == limit) orders.last().createdAt.toString() else null
            call.respond(OrderListResponse(orders.map { it.toResponse() }, nextCursor))
        }
    }
}

private fun com.example.domain.Order.toResponse() = OrderResponse(
    orderId = id.toString(),
    ticker = ticker,
    side = side,
    orderType = orderType,
    status = status,
    quantity = quantity.toPlainString(),
    executedPrice = executedPrice?.toPlainString(),
    createdAt = createdAt.toString(),
)
