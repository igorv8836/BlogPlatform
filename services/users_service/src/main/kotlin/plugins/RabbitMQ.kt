package com.example.plugins

import com.example.config.ServiceConfig
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicConsume
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicProperties
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.github.damir.denis.tudor.ktor.server.rabbitmq.rabbitMQ
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers

fun Routing.configureRabbitRouting(
    application: Application,
    config: ServiceConfig,
    routing: String
) {
    rabbitmq {
        get("/rabbitmq") {
            basicPublish {
                exchange = config.ktor.rabbitmq.exchange
                routingKey = routing
                properties = basicProperties {
                    correlationId = "jetbrains"
                    type = "plugin"
                    headers = mapOf("ktor" to "rabbitmq")
                }
                message { "Hello Ktor!" }
            }

            call.respondText("Hello RabbitMQ!")
        }
    }

    rabbitmq {
        basicConsume {
            autoAck = true
            queue = config.ktor.rabbitmq.queue
            dispatcher = Dispatchers.rabbitMQ
            coroutinePollSize = 100

            deliverCallback<String> { message ->
                application.log.info("Received message: $message")
            }

            deliverFailureCallback { message ->
                application.log.error("Received undeliverable message (deserialization failed): ${message.body.toString(Charsets.UTF_8)}")
            }
        }
    }
}