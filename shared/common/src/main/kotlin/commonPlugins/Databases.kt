package com.example.commonPlugins

import com.example.config.ServiceConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

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
