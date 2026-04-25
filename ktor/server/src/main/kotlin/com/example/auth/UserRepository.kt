package com.example.auth

import com.example.domain.User
import java.sql.Connection
import java.time.Instant
import java.util.UUID

class UserRepository {

    fun findByEmail(conn: Connection, email: String): User? {
        val sql = "SELECT id, email, full_name, role, created_at FROM users WHERE email = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, email)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.toUser() else null
        }
    }

    fun findById(conn: Connection, id: UUID): User? {
        val sql = "SELECT id, email, full_name, role, created_at FROM users WHERE id = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, id)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.toUser() else null
        }
    }

    fun getPasswordHash(conn: Connection, email: String): String? {
        val sql = "SELECT password_hash FROM users WHERE email = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, email)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getString("password_hash") else null
        }
    }

    fun create(conn: Connection, email: String, passwordHash: String, fullName: String?): User {
        val sql = """
            INSERT INTO users (email, password_hash, full_name)
            VALUES (?, ?, ?)
            RETURNING id, email, full_name, role, created_at
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, email)
            stmt.setString(2, passwordHash)
            stmt.setString(3, fullName)
            val rs = stmt.executeQuery()
            if (rs.next()) return rs.toUser()
            throw IllegalStateException("User insert returned no rows")
        }
    }

    fun createAccount(conn: Connection, userId: UUID) {
        val sql = "INSERT INTO accounts (user_id, currency, cash_balance) VALUES (?, 'RUB', 100000.0000)"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, userId)
            stmt.executeUpdate()
        }
    }

    private fun java.sql.ResultSet.toUser() = User(
        id = UUID.fromString(getString("id")),
        email = getString("email"),
        fullName = getString("full_name"),
        role = getString("role"),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
