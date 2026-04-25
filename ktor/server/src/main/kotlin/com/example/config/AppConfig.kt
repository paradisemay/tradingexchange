package com.example.config

data class AppConfig(
    val postgres: PostgresConfig,
    val jwt: JwtConfig,
    val redis: RedisConfig,
) {
    companion object {
        fun fromEnvironment(): AppConfig = AppConfig(
            postgres = PostgresConfig(
                host = env("POSTGRES_HOST", "localhost"),
                port = env("POSTGRES_PORT", "5432").toInt(),
                db = env("POSTGRES_DB", "broker"),
                user = env("POSTGRES_USER", "broker_app"),
                password = env("POSTGRES_PASSWORD", "broker_app"),
                maxPoolSize = env("POSTGRES_MAX_POOL_SIZE", "30").toInt(),
                connectionTimeoutMs = env("POSTGRES_CONNECTION_TIMEOUT_MS", "3000").toLong(),
            ),
            jwt = JwtConfig(
                secret = env("JWT_SECRET", "dev-secret-change-me"),
                accessTtlMinutes = env("JWT_ACCESS_TTL_MINUTES", "15").toLong(),
                refreshTtlDays = env("JWT_REFRESH_TTL_DAYS", "30").toLong(),
                issuer = env("JWT_ISSUER", "ktor-broker"),
                audience = env("JWT_AUDIENCE", "ktor-broker-clients"),
            ),
            redis = RedisConfig(
                host = env("REDIS_HOST", "localhost"),
                port = env("REDIS_PORT", "6379").toInt(),
                password = System.getenv("REDIS_PASSWORD"),
            ),
        )

        private fun env(name: String, default: String) = System.getenv(name) ?: default
    }
}

data class PostgresConfig(
    val host: String,
    val port: Int,
    val db: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
    val connectionTimeoutMs: Long,
) {
    val jdbcUrl: String get() = "jdbc:postgresql://$host:$port/$db"
}

data class JwtConfig(
    val secret: String,
    val accessTtlMinutes: Long,
    val refreshTtlDays: Long,
    val issuer: String,
    val audience: String,
)

data class RedisConfig(
    val host: String,
    val port: Int,
    val password: String?,
) {
    val uri: String get() = if (password != null)
        "redis://:$password@$host:$port"
    else
        "redis://$host:$port"
}
