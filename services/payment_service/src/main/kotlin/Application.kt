package com.example

import com.example.commonPlugins.*
import com.example.config.ConfigName
import com.example.config.ServiceConfig
import com.example.config.getServiceConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import plugins.configureRabbitRouting

fun main(args: Array<String>) {
    val config = getServiceConfig(ConfigName.PAYMENT_SERVICE)
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
//            dataModule(),
        ),
    )

    configureSecurity(config)
    configureCommonRouting()

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
}
