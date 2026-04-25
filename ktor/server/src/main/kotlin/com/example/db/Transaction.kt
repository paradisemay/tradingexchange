package com.example.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import javax.sql.DataSource

suspend fun <T> DataSource.withTransaction(block: (Connection) -> T): T =
    withContext(Dispatchers.IO) {
        connection.use { conn ->
            conn.autoCommit = false
            try {
                val result = block(conn)
                conn.commit()
                result
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

suspend fun <T> DataSource.withConnection(block: (Connection) -> T): T =
    withContext(Dispatchers.IO) {
        connection.use { conn -> block(conn) }
    }
