package com.example.plugins

import com.example.auth.JwtUtil
import com.example.config.JwtConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

@Serializable
private data class AuthError(val errorCode: String, val message: String)

fun Application.configureSecurity(cfg: JwtConfig, jwtUtil: JwtUtil) {
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
