package com.example.auth

import com.example.config.JwtConfig
import com.example.db.withTransaction
import com.example.domain.ErrorCode
import com.example.domain.AppException
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

@Serializable
data class TokenPair(val accessToken: String, val refreshToken: String)

@Serializable
data class RegisterRequest(val email: String, val password: String, val fullName: String? = null)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class RegisterResponse(val userId: String, val accessToken: String, val refreshToken: String)

class AuthService(
    private val ds: DataSource,
    private val jwtUtil: JwtUtil,
    private val jwtCfg: JwtConfig,
    private val userRepo: UserRepository = UserRepository(),
    private val sessionRepo: SessionRepository = SessionRepository(),
) {

    suspend fun register(req: RegisterRequest): RegisterResponse {
        validateRegisterInput(req)
        return ds.withTransaction { conn ->
            if (userRepo.findByEmail(conn, req.email) != null)
                throw AppException(ErrorCode.CONFLICT, "Email already registered")
            val hash = hashPassword(req.password)
            val user = userRepo.create(conn, req.email, hash, req.fullName)
            userRepo.createAccount(conn, user.id)
            val (access, refresh) = issueTokens(conn, user.id)
            RegisterResponse(user.id.toString(), access, refresh)
        }
    }

    suspend fun login(req: LoginRequest): TokenPair {
        return ds.withTransaction { conn ->
            val hash = userRepo.getPasswordHash(conn, req.email)
                ?: throw AppException(ErrorCode.UNAUTHORIZED, "Invalid credentials")
            if (!verifyPassword(req.password, hash))
                throw AppException(ErrorCode.UNAUTHORIZED, "Invalid credentials")
            val user = userRepo.findByEmail(conn, req.email)!!
            val (access, refresh) = issueTokens(conn, user.id)
            TokenPair(access, refresh)
        }
    }

    suspend fun refresh(req: RefreshRequest): TokenPair {
        return ds.withTransaction { conn ->
            val tokenHash = hashToken(req.refreshToken)
            val session = sessionRepo.findActiveByHash(conn, tokenHash)
                ?: throw AppException(ErrorCode.UNAUTHORIZED, "Refresh token is invalid or expired")
            sessionRepo.revoke(conn, session.id)
            val (access, refresh) = issueTokens(conn, session.userId)
            TokenPair(access, refresh)
        }
    }

    suspend fun logout(userId: UUID, req: LogoutRequest) {
        ds.withTransaction { conn ->
            val tokenHash = hashToken(req.refreshToken)
            val session = sessionRepo.findActiveByHash(conn, tokenHash)
            if (session != null && session.userId == userId)
                sessionRepo.revoke(conn, session.id)
        }
    }

    private fun issueTokens(conn: java.sql.Connection, userId: UUID): Pair<String, String> {
        val accessToken = jwtUtil.generateAccessToken(userId)
        val refreshToken = UUID.randomUUID().toString()
        val tokenHash = hashToken(refreshToken)
        val expiresAt = Instant.now().plusSeconds(jwtCfg.refreshTtlDays * 24 * 3600)
        sessionRepo.create(conn, userId, tokenHash, expiresAt)
        return accessToken to refreshToken
    }

    private fun validateRegisterInput(req: RegisterRequest) {
        if (req.email.isBlank() || !req.email.contains('@'))
            throw AppException(ErrorCode.VALIDATION_ERROR, "Invalid email format")
        if (req.password.length < 8)
            throw AppException(ErrorCode.VALIDATION_ERROR, "Password must be at least 8 characters")
    }
}
