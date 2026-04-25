package com.example.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.config.JwtConfig
import java.util.Date
import java.util.UUID

class JwtUtil(private val cfg: JwtConfig) {

    private val algorithm = Algorithm.HMAC256(cfg.secret)

    fun generateAccessToken(userId: UUID): String = JWT.create()
        .withIssuer(cfg.issuer)
        .withAudience(cfg.audience)
        .withSubject(userId.toString())
        .withClaim("userId", userId.toString())
        .withExpiresAt(Date(System.currentTimeMillis() + cfg.accessTtlMinutes * 60 * 1000))
        .sign(algorithm)

    fun verifier() = JWT.require(algorithm)
        .withIssuer(cfg.issuer)
        .withAudience(cfg.audience)
        .build()

    fun extractUserId(token: String): UUID? = runCatching {
        val decoded = verifier().verify(token)
        UUID.fromString(decoded.getClaim("userId").asString())
    }.getOrNull()
}
