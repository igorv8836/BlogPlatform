package com.example

import com.example.clients.clientsModule
import com.example.commonPlugins.*
import com.example.config.ConfigName
import com.example.config.ServiceConfig
import com.example.config.getServiceConfig
import com.example.data.dataModule
import com.example.data.db.tables.BanTable
import com.example.data.db.tables.FollowTable
import com.example.data.db.tables.UserTable
import com.example.plugins.configureRabbitRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import com.example.routes.userRouting

fun main(args: Array<String>) {
    val config = getServiceConfig(ConfigName.USER_SERVICE)
    embeddedServer(
        factory = Netty,
        port = config.ktor.deployment.port,
        host = config.ktor.deployment.host,
        module = { module(config) }
    ).start(wait = true)
}

fun Application.module(config: ServiceConfig) {
    configureOpenApi()
    configureMonitoring()
    configureSerialization()
    configureKoin(
        otherModules = listOf(
            coreClientModule(),
            clientsModule(),
            dataModule(),
        ),
    )

    val routing = config.ktor.jwt.audience
    configureRabbitMQ(
        config = config,
        configuration = {
            configureRabbitRouting(
                application = this@module,
                config = config,
                routing = routing
            )
        },
        routing = routing,
    )

    configureSecurity(config)
    configureCommonRouting()
    DatabaseFactory.initializationDatabase(
        config = config,
        tables = arrayOf(
            UserTable,
            FollowTable,
            BanTable
        )
    )
    userRouting(config)
}
