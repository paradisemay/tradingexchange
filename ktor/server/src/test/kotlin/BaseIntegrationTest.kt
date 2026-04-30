package com.example

import com.example.config.AppConfig
import com.example.config.JwtConfig
import com.example.config.PostgresConfig
import com.example.config.RedisConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@Testcontainers
abstract class BaseIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:16").apply {
            withDatabaseName("broker")
            withUsername("broker_app")
            withPassword("testpass")
        }

        @Container
        @JvmStatic
        val redis: GenericContainer<Nothing> = GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
        }

        // Строим AppConfig напрямую из портов контейнеров — без env-переменных
        fun buildConfig(): AppConfig = AppConfig(
            postgres = PostgresConfig(
                host = postgres.host,
                port = postgres.firstMappedPort,
                db = postgres.databaseName,
                user = postgres.username,
                password = postgres.password,
                maxPoolSize = 5,
                connectionTimeoutMs = 3000,
            ),
            jwt = JwtConfig(
                secret = "test-secret-for-integration-32chars!!",
                accessTtlMinutes = 15,
                refreshTtlDays = 30,
                issuer = "ktor-broker",
                audience = "ktor-broker-clients",
            ),
            redis = RedisConfig(
                host = redis.host,
                port = redis.firstMappedPort,
                password = null,
                streamName = "stream:quotes:v1",
                consumerGroup = "cg:ktor:quotes",
                consumerName = "ktor-instance-test",
            ),
        )

        fun uniqueEmail() = "test-${UUID.randomUUID()}@example.com"
    }
}
