package com.example

import com.example.config.JwtConfig
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

@Serializable
private data class AuthError(val errorCode: String, val message: String, val traceId: String = "")

fun Application.configureSecurity(cfg: JwtConfig, jwtUtil: com.example.auth.JwtUtil) {
    authentication {
        jwt("auth-jwt") {
            realm = "broker-api"
            verifier(jwtUtil.verifier())
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null)
                    JWTPrincipal(credential.payload)
                else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, AuthError("UNAUTHORIZED", "Token is missing or invalid"))
            }
        }
    }
}
