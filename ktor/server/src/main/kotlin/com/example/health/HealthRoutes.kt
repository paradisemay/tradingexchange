package com.example.health

import io.lettuce.core.RedisClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.sql.DataSource

@Serializable
data class LivenessResponse(val status: String)

@Serializable
data class ReadinessResponse(val status: String, val db: String, val redis: String)

fun Route.healthRoutes(ds: DataSource, redisClient: RedisClient) {
    get("/health/live") {
        call.respond(HttpStatusCode.OK, LivenessResponse("UP"))
    }

    get("/health/ready") {
        val dbStatus = checkDatabase(ds)
        val redisStatus = checkRedis(redisClient)
        val overall = if (dbStatus == "UP" && redisStatus == "UP") "UP" else "DOWN"
        val httpStatus = if (overall == "UP") HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        call.respond(httpStatus, ReadinessResponse(overall, dbStatus, redisStatus))
    }
}

private suspend fun checkDatabase(ds: DataSource): String =
    withContext(Dispatchers.IO) {
        runCatching {
            ds.connection.use { conn -> conn.prepareStatement("SELECT 1").executeQuery() }
            "UP"
        }.getOrElse { "DOWN" }
    }

private suspend fun checkRedis(client: RedisClient): String =
    withContext(Dispatchers.IO) {
        runCatching {
            val conn = client.connect()
            val pong = conn.sync().ping()
            conn.close()
            if (pong == "PONG") "UP" else "DOWN"
        }.getOrElse { "DOWN" }
    }
