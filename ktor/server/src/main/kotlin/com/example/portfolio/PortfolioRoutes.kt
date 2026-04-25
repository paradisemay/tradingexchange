package com.example.portfolio

import com.example.db.withConnection
import com.example.domain.AppException
import com.example.domain.ErrorCode
import com.example.quotes.PriceCache
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.sql.DataSource

@Serializable
data class PortfolioPositionResponse(
    val ticker: String,
    val quantity: String,
    val avgPrice: String,
    val currentPrice: String?,
    val currency: String,
)

@Serializable
data class CashResponse(val currency: String, val available: String)

@Serializable
data class PortfolioResponse(
    val positions: List<PortfolioPositionResponse>,
    val cash: CashResponse,
)

@Serializable
data class UserProfileResponse(val userId: String, val email: String, val fullName: String?, val role: String)

fun Route.portfolioRoutes(
    ds: DataSource,
    priceCache: PriceCache,
    repo: PortfolioRepository = PortfolioRepository(),
) {
    authenticate("auth-jwt") {
        get("/api/v1/me") {
            val userId = call.userId()
            val user = ds.withConnection { conn ->
                com.example.auth.UserRepository().findById(conn, userId)
                    ?: throw AppException(ErrorCode.UNAUTHORIZED, "User not found")
            }
            call.respond(UserProfileResponse(user.id.toString(), user.email, user.fullName, user.role))
        }

        get("/api/v1/portfolio") {
            val userId = call.userId()
            val (account, positions) = ds.withConnection { conn ->
                val acc = repo.findAccount(conn, userId)
                    ?: throw AppException(ErrorCode.INTERNAL_ERROR, "Account not found")
                val pos = repo.findPositions(conn, userId)
                acc to pos
            }
            val posResponses = positions.map { p ->
                PortfolioPositionResponse(
                    ticker = p.ticker,
                    quantity = p.quantity.toPlainString(),
                    avgPrice = p.avgPrice.toPlainString(),
                    currentPrice = priceCache.getPrice(p.ticker)?.toPlainString(),
                    currency = "RUB",
                )
            }
            call.respond(PortfolioResponse(posResponses, CashResponse("RUB", account.cashBalance.toPlainString())))
        }
    }
}

fun io.ktor.server.application.ApplicationCall.userId(): UUID =
    principal<JWTPrincipal>()
        ?.payload?.getClaim("userId")?.asString()
        ?.let { UUID.fromString(it) }
        ?: throw AppException(ErrorCode.UNAUTHORIZED, "Missing or invalid token")
