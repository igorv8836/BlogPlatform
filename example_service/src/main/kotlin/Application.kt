package com.example

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File

fun main(args: Array<String>) {
    val configFile = File("example_service/src/main/resources/application.conf")
    val hoconConfig = ConfigFactory.parseFile(configFile)
    val config = HoconApplicationConfig(hoconConfig)

    val host = config.property("ktor.deployment.host").getString()
    val port = config.property("ktor.deployment.port").getString().toInt()
    embeddedServer(Netty, port = port, host = host, module = { module(config) }).start(wait = true)
}

fun Application.module(config: HoconApplicationConfig) {
    configureHTTP()
    configureSecurity()
    configureMonitoring()
    configureSerialization()
    configureDatabases(config)
    configureFrameworks()
    configureRouting()
}
