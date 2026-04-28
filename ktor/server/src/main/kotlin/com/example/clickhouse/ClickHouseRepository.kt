package com.example.clickhouse

import com.example.config.ClickHouseConfig
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Instant
import java.util.Properties

@Serializable
data class LinePoint(val timestampMs: Long, val price: Double)

@Serializable
data class CandlePoint(
    val timestampMs: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
)

@Serializable
data class ChartSummary(
    val minPrice: Double,
    val maxPrice: Double,
    val firstPrice: Double,
    val lastPrice: Double,
    val points: Long,
)

class ClickHouseRepository(private val config: ClickHouseConfig) {
    private val log = LoggerFactory.getLogger(ClickHouseRepository::class.java)

    fun getLine(ticker: String, from: Instant, to: Instant, limit: Int = 1000): List<LinePoint> {
        val sql = """
            SELECT event_time, price
            FROM trading.quotes_raw
            WHERE symbol = ? AND event_time >= ? AND event_time < ?
            ORDER BY event_time
            LIMIT ?
        """.trimIndent()

        return query(sql) { stmt ->
            stmt.setString(1, ticker)
            stmt.setTimestamp(2, Timestamp.from(from))
            stmt.setTimestamp(3, Timestamp.from(to))
            stmt.setInt(4, limit)
            val rs = stmt.executeQuery()
            buildList {
                while (rs.next()) {
                    add(LinePoint(rs.getTimestamp(1).toInstant().toEpochMilli(), rs.getDouble(2)))
                }
            }
        }
    }

    fun getCandles(ticker: String, from: Instant, to: Instant, limit: Int = 1000): List<CandlePoint> {
        val sql = """
            SELECT bucket, open, high, low, close, volume
            FROM trading.quotes_ohlc_1m_read
            WHERE symbol = ? AND bucket >= ? AND bucket < ?
            ORDER BY bucket
            LIMIT ?
        """.trimIndent()

        return query(sql) { stmt ->
            stmt.setString(1, ticker)
            stmt.setTimestamp(2, Timestamp.from(from))
            stmt.setTimestamp(3, Timestamp.from(to))
            stmt.setInt(4, limit)
            val rs = stmt.executeQuery()
            buildList {
                while (rs.next()) {
                    add(CandlePoint(
                        timestampMs = rs.getTimestamp(1).toInstant().toEpochMilli(),
                        open = rs.getDouble(2),
                        high = rs.getDouble(3),
                        low = rs.getDouble(4),
                        close = rs.getDouble(5),
                        volume = rs.getLong(6),
                    ))
                }
            }
        }
    }

    fun getSummary(ticker: String, from: Instant, to: Instant): ChartSummary? {
        val sql = """
            SELECT min(price), max(price), argMin(price, event_time), argMax(price, event_time), count()
            FROM trading.quotes_raw
            WHERE symbol = ? AND event_time >= ? AND event_time < ?
        """.trimIndent()

        return query(sql) { stmt ->
            stmt.setString(1, ticker)
            stmt.setTimestamp(2, Timestamp.from(from))
            stmt.setTimestamp(3, Timestamp.from(to))
            val rs = stmt.executeQuery()
            if (rs.next() && rs.getLong(5) > 0) {
                ChartSummary(
                    minPrice = rs.getDouble(1),
                    maxPrice = rs.getDouble(2),
                    firstPrice = rs.getDouble(3),
                    lastPrice = rs.getDouble(4),
                    points = rs.getLong(5),
                )
            } else null
        }
    }

    private fun <T> query(sql: String, block: (java.sql.PreparedStatement) -> T): T {
        val props = Properties().apply {
            setProperty("user", config.user)
            setProperty("password", config.password)
        }
        DriverManager.getConnection(config.jdbcUrl, props).use { conn ->
            conn.prepareStatement(sql).use { stmt -> return block(stmt) }
        }
    }
}
