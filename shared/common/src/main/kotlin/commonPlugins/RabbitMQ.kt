package com.example.commonPlugins

import com.example.config.ServiceConfig
import io.github.damir.denis.tudor.ktor.server.rabbitmq.RabbitMQ
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.exchangeDeclare
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.queueBind
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.queueDeclare
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

fun Application.configureRabbitMQ(
    config: ServiceConfig,
    configuration: Routing.() -> Unit,
    routing: String
) {
    val exceptionHandler = CoroutineExceptionHandler { _, throwable -> log.error("ExceptionHandler got $throwable") }
    val rabbitMQScope = CoroutineScope(SupervisorJob() + exceptionHandler)

    install(RabbitMQ) {
        uri = config.ktor.rabbitmq.uri
        defaultConnectionName = config.ktor.rabbitmq.defaultConnectionName
        dispatcherThreadPollSize = config.ktor.rabbitmq.dispatcherThreadPollSize
        tlsEnabled = config.ktor.rabbitmq.tls.enabled
        scope = rabbitMQScope
    }

    rabbitmq {
        queueBind {
            queue = "dlq"
            exchange = "dlx"
            routingKey = "dlq-dlx"
            exchangeDeclare {
                exchange = "dlx"
                type = "direct"
            }
            queueDeclare {
                queue = "dlq"
                durable = true
            }
        }
    }

    rabbitmq {
        queueBind {
            queue = config.ktor.rabbitmq.queue
            exchange = config.ktor.rabbitmq.exchange
            routingKey = routing
            exchangeDeclare {
                exchange = config.ktor.rabbitmq.exchange
                type = "direct"
            }
            queueDeclare {
                queue = config.ktor.rabbitmq.queue
                arguments = mapOf(
                    "x-dead-letter-exchange" to "dlx",
                    "x-dead-letter-routing-key" to "dlq-dlx"
                )
            }
        }
    }

    routing {
        configuration()
    }
}
