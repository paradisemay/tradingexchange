package com.example.auth

import com.example.domain.Session
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class SessionRepository {

    fun create(conn: Connection, userId: UUID, tokenHash: String, expiresAt: Instant): Session {
        val sql = """
            INSERT INTO sessions (user_id, refresh_token_hash, expires_at)
            VALUES (?, ?, ?)
            RETURNING id, user_id, refresh_token_hash, expires_at, revoked_at
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, userId)
            stmt.setString(2, tokenHash)
            stmt.setTimestamp(3, Timestamp.from(expiresAt))
            val rs = stmt.executeQuery()
            if (rs.next()) return rs.toSession()
            throw IllegalStateException("Session insert returned no rows")
        }
    }

    fun findActiveByHash(conn: Connection, tokenHash: String): Session? {
        val sql = """
            SELECT id, user_id, refresh_token_hash, expires_at, revoked_at
            FROM sessions
            WHERE refresh_token_hash = ?
              AND revoked_at IS NULL
              AND expires_at > NOW()
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, tokenHash)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.toSession() else null
        }
    }

    fun revoke(conn: Connection, sessionId: UUID) {
        val sql = "UPDATE sessions SET revoked_at = NOW() WHERE id = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, sessionId)
            stmt.executeUpdate()
        }
    }

    private fun java.sql.ResultSet.toSession() = Session(
        id = UUID.fromString(getString("id")),
        userId = UUID.fromString(getString("user_id")),
        refreshTokenHash = getString("refresh_token_hash"),
        expiresAt = getTimestamp("expires_at").toInstant(),
        revokedAt = getTimestamp("revoked_at")?.toInstant(),
    )
}
