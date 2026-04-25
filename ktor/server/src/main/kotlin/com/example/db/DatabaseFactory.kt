package com.example.db

import com.example.config.PostgresConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

object DatabaseFactory {

    fun create(config: PostgresConfig): HikariDataSource {
        runMigrations(config)
        return createPool(config)
    }

    private fun runMigrations(config: PostgresConfig) {
        Flyway.configure()
            .dataSource(config.jdbcUrl, config.user, config.password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(false)
            .load()
            .migrate()
    }

    private fun createPool(config: PostgresConfig): HikariDataSource {
        val hc = HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            connectionTimeout = config.connectionTimeoutMs
            driverClassName = "org.postgresql.Driver"
            poolName = "broker-pool"
            isAutoCommit = true
        }
        return HikariDataSource(hc)
    }
}
