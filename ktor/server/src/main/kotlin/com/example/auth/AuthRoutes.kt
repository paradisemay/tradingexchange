package com.example.auth

import com.example.domain.AppException
import com.example.domain.ErrorCode
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.authRoutes(authService: AuthService) {
    route("/api/v1/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            val resp = authService.register(req)
            call.respond(HttpStatusCode.Created, resp)
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            val resp = authService.login(req)
            call.respond(HttpStatusCode.OK, resp)
        }

        post("/refresh") {
            val req = call.receive<RefreshRequest>()
            val resp = authService.refresh(req)
            call.respond(HttpStatusCode.OK, resp)
        }

        authenticate("auth-jwt") {
            post("/logout") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()
                    ?.let { UUID.fromString(it) }
                    ?: throw AppException(ErrorCode.UNAUTHORIZED, "Missing token")
                val req = call.receive<LogoutRequest>()
                authService.logout(userId, req)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
