package com.example.instruments

import com.example.db.withConnection
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import javax.sql.DataSource

@Serializable
data class InstrumentResponse(
    val ticker: String,
    val name: String,
    val currency: String,
    val lotSize: Int,
    val isActive: Boolean,
    val lastPrice: String?,
)

fun Route.instrumentRoutes(ds: DataSource, repo: InstrumentRepository = InstrumentRepository()) {
    authenticate("auth-jwt") {
        get("/api/v1/instruments") {
            val query = call.request.queryParameters["query"]
            val instruments = ds.withConnection { conn -> repo.findAll(conn, query) }
            call.respond(HttpStatusCode.OK, instruments.map { it.toResponse() })
        }
    }
}

private fun com.example.domain.Instrument.toResponse() = InstrumentResponse(
    ticker = ticker,
    name = name,
    currency = currency,
    lotSize = lotSize,
    isActive = isActive,
    lastPrice = lastPrice?.toPlainString(),
)
