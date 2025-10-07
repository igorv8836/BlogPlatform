package com.example.commonPlugins

import com.example.config.ServiceConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    fun initializationDatabase(
        config: ServiceConfig,
        tables: Array<Table>,
    ) {
        Database.connect(getHikariDatasource(config))

        transaction {
            SchemaUtils.create(
                tables = tables,
            )

            SchemaUtils.createMissingTablesAndColumns(
                tables = tables,
            )
        }
    }

    private fun getHikariDatasource(myConfig: ServiceConfig): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = myConfig.storage.driverClassName
        config.jdbcUrl = myConfig.storage.jdbcURL
        config.username = myConfig.storage.user
        config.password = myConfig.storage.password
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

}