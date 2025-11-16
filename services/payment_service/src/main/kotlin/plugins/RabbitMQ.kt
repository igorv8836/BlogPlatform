package plugins

import com.example.config.ServiceConfig
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.*
import io.github.damir.denis.tudor.ktor.server.rabbitmq.rabbitMQ
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import payment.request.PaymentAsyncRequest
import wallet.request.WalletAsyncRequest

fun Routing.configureRabbitRouting(
    application: Application,
    config: ServiceConfig,
    routing: String
) {
    rabbitmq {
        basicConsume {
            autoAck = false
            queue = config.ktor.rabbitmq.queue
            dispatcher = Dispatchers.rabbitMQ
            coroutinePollSize = 100

            deliverCallback<PaymentAsyncRequest> { message ->
                application.log.info("Requested information: ${message.body}")

                val replyTo = message.properties.replyTo
                if (replyTo.isNullOrBlank()) {
                    application.log.warn("Received request without replyTo — cannot send response")
                    basicAck { deliveryTag = message.envelope.deliveryTag }
                    return@deliverCallback
                }

                application.log.info("ReplyTo: $replyTo")

                basicAck { deliveryTag = message.envelope.deliveryTag }

                when (val paymentRequest = message.body) {
                    is PaymentAsyncRequest.CreditFundsRequest -> {

                        basicPublish {
                            exchange = config.ktor.rabbitmq.exchange
                            routingKey = replyTo
                            properties = basicProperties {
                                correlationId = message.properties.correlationId
                            }
                            message {
                                WalletAsyncRequest.CreditRequest(
                                    paymentRequest.userId,
                                    paymentRequest.amount
                                ) as WalletAsyncRequest
                            }
                        }
                    }
                    is PaymentAsyncRequest.DebitFundsRequest -> {
                        basicPublish {
                            exchange = config.ktor.rabbitmq.exchange
                            routingKey = replyTo
                            properties = basicProperties {
                                correlationId = message.properties.correlationId
                            }
                            message {
                                WalletAsyncRequest.DebitRequest(
                                    paymentRequest.userId,
                                    paymentRequest.amount
                                ) as WalletAsyncRequest
                            }
                        }
                    }
                    is PaymentAsyncRequest.TransferFundsRequest -> {
                        basicPublish {
                            exchange = config.ktor.rabbitmq.exchange
                            routingKey = replyTo
                            properties = basicProperties {
                                correlationId = message.properties.correlationId
                            }
                            message {
                                WalletAsyncRequest.DebitRequest(
                                    paymentRequest.fromUserId,
                                    paymentRequest.amount
                                ) as WalletAsyncRequest
                            }
                        }

                        basicPublish {
                            exchange = config.ktor.rabbitmq.exchange
                            routingKey = replyTo
                            properties = basicProperties {
                                correlationId = message.properties.correlationId
                            }
                            message {
                                WalletAsyncRequest.CreditRequest(
                                    paymentRequest.toUserId,
                                    paymentRequest.amount
                                ) as WalletAsyncRequest
                            }
                        }
                    }
                }
            }

            deliverFailureCallback { message ->
                application.log.error("Received undeliverable message (deserialization failed): ${message.body.toString(Charsets.UTF_8)}")
            }
        }
    }
}