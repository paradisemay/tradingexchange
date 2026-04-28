package com.example.clickhouse

import com.example.domain.AppException
import com.example.domain.ErrorCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.Instant
import java.time.format.DateTimeParseException

fun Route.chartRoutes(repo: ClickHouseRepository) {
    authenticate("auth-jwt") {
        get("/api/v1/charts/{ticker}/line") {
            val ticker = call.parameters["ticker"]
                ?: throw AppException(ErrorCode.VALIDATION_ERROR, "ticker is required")
            val from = parseInstant(call.request.queryParameters["from"], "from")
            val to = parseInstant(call.request.queryParameters["to"], "to")
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 5000) ?: 1000

            call.respond(HttpStatusCode.OK, repo.getLine(ticker, from, to, limit))
        }

        get("/api/v1/charts/{ticker}/candles") {
            val ticker = call.parameters["ticker"]
                ?: throw AppException(ErrorCode.VALIDATION_ERROR, "ticker is required")
            val from = parseInstant(call.request.queryParameters["from"], "from")
            val to = parseInstant(call.request.queryParameters["to"], "to")
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 1440) ?: 500

            call.respond(HttpStatusCode.OK, repo.getCandles(ticker, from, to, limit))
        }

        get("/api/v1/charts/{ticker}/summary") {
            val ticker = call.parameters["ticker"]
                ?: throw AppException(ErrorCode.VALIDATION_ERROR, "ticker is required")
            val from = parseInstant(call.request.queryParameters["from"], "from")
            val to = parseInstant(call.request.queryParameters["to"], "to")

            val summary = repo.getSummary(ticker, from, to)
                ?: return@get call.respond(HttpStatusCode.NoContent)
            call.respond(HttpStatusCode.OK, summary)
        }
    }
}

private fun parseInstant(value: String?, paramName: String): Instant {
    if (value.isNullOrBlank()) throw AppException(ErrorCode.VALIDATION_ERROR, "$paramName is required (ISO-8601)")
    return try {
        Instant.parse(value)
    } catch (e: DateTimeParseException) {
        throw AppException(ErrorCode.VALIDATION_ERROR, "$paramName must be ISO-8601, e.g. 2025-01-01T00:00:00Z")
    }
}
