package com.example

import com.example.auth.AuthService
import com.example.auth.JwtUtil
import com.example.auth.authRoutes
import com.example.config.AppConfig
import com.example.db.DatabaseFactory
import com.example.health.healthRoutes
import com.example.instruments.instrumentRoutes
import com.example.orders.OrderService
import com.example.orders.orderRoutes
import com.example.plugins.configureMonitoring
import com.example.plugins.configureOpenTelemetry
import com.example.plugins.configureSecurity
import com.example.plugins.configureSerialization
import com.example.plugins.configureStatusPages
import com.example.plugins.configureWebsockets
import com.example.portfolio.portfolioRoutes
import com.example.quotes.PriceCache
import com.example.quotes.RedisSubscriber
import com.example.quotes.WebSocketManager
import com.example.quotes.quotesWebSocket
import com.example.transactions.transactionRoutes
import io.lettuce.core.RedisClient
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun Application.module() {
    val config = AppConfig.fromEnvironment()
    val jwtUtil = JwtUtil(config.jwt)

    val dataSource = DatabaseFactory.create(config.postgres)
    val redisClient = RedisClient.create(config.redis.uri)

    val priceCache = PriceCache()
    val wsManager = WebSocketManager()
    val json = Json { ignoreUnknownKeys = true }

    val redisSubscriber = RedisSubscriber(config.redis, priceCache, wsManager, json)

    configureMonitoring()
    configureSerialization()
    configureSecurity(config.jwt, jwtUtil)
    configureWebsockets()
    configureStatusPages()
    configureOpenTelemetry()

    val authService = AuthService(dataSource, jwtUtil, config.jwt)
    val orderService = OrderService(dataSource, priceCache)

    routing {
        healthRoutes(dataSource, redisClient)
        authRoutes(authService)
        portfolioRoutes(dataSource, priceCache)
        instrumentRoutes(dataSource)
        orderRoutes(dataSource, orderService)
        transactionRoutes(dataSource)
        quotesWebSocket(wsManager, jwtUtil, json)
    }

    redisSubscriber.start()

    monitor.subscribe(ApplicationStopped) {
        redisSubscriber.stop()
        dataSource.close()
        redisClient.shutdown()
    }
}
