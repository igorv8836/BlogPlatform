package routes

import com.example.config.ServiceConfig
import com.example.utils.userIdOrNull
import data.repositories.PaymentMethodRepository
import data.repositories.SubscriptionRepository
import data.repositories.WalletRepository
import data.repositories.WithdrawalRequestRepository
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicProperties
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import payment.request.PaymentAsyncRequest
import wallet.Currency
import wallet.request.AddPaymentMethodRequest
import wallet.request.RequestWithdrawalRequest
import wallet.request.SubscribeToAuthorRequest
import wallet.request.SupportAuthorRequest
import wallet.response.SupportAuthorResponse
import java.util.*

const val PAYMENT_ROUTING_KEY = "payment-service"

fun Application.configureWalletRouting(config: ServiceConfig) {
    val walletRepository by inject<WalletRepository>()
    val paymentMethodRepository by inject<PaymentMethodRepository>()
    val withdrawalRequestRepository by inject<WithdrawalRequestRepository>()
    val subscriptionRepository by inject<SubscriptionRepository>()

    routing {
        authenticate("jwt") {
            route("/api/v1/wallet") {
                post {
                    val userId: Long = call.userIdOrNull()?.toLong() ?: 125
                    call.respond(walletRepository.createWallet(userId))
                }

                get("/balance") {
                    val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                    call.respond(walletRepository.getUserBalance(userId))
                }

                post("/payment-method") {
                    val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                    val request = call.receive<AddPaymentMethodRequest>()
                    call.respond(paymentMethodRepository.addPaymentMethod(userId, request))
                }

                delete("/payment-method") {
                    val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                    val methodId = UUID.fromString(call.parameters["methodId"])

                    call.respond(paymentMethodRepository.deletePaymentMethod(methodId, userId))
                }

                post("/payment-method/select-default") {
                    val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                    val methodId = UUID.fromString(call.parameters["methodId"])

                    call.respond(paymentMethodRepository.selectDefaultPaymentMethod(methodId, userId))
                }

                // WITH PAYMENT_SERVICE
                post("/withdrawal") {
                    val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                    val request = call.receive<RequestWithdrawalRequest>()
                    val balance = walletRepository.getUserBalance(userId).currentBalance
                    if (balance < request.amount) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Insufficient funds"))
                        return@post
                    }

                    rabbitmq {
                        basicPublish {
                            exchange = config.ktor.rabbitmq.exchange
                            routingKey = PAYMENT_ROUTING_KEY
                            properties = basicProperties {
                                correlationId = UUID.randomUUID().toString()
                                replyTo = config.ktor.jwt.audience
                            }
                            message {
                                PaymentAsyncRequest.DebitFundsRequest(
                                    userId = userId,
                                    amount = request.amount,
                                    currency = request.currency,
                                    sourceDebitId = request.paymentMethodId
                                ) as PaymentAsyncRequest
                            }
                        }
                    }

                    call.respond(withdrawalRequestRepository.requestWithdrawal(userId, request))
                }

                post("/subscription") {
                    val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                    val request = call.receive<SubscribeToAuthorRequest>()

                    rabbitmq {
                        basicPublish {
                            exchange = config.ktor.rabbitmq.exchange
                            routingKey = PAYMENT_ROUTING_KEY
                            properties = basicProperties {
                                correlationId = UUID.randomUUID().toString()
                                replyTo = config.ktor.jwt.audience
                            }
                            message {
                                PaymentAsyncRequest.TransferFundsRequest(
                                    fromUserId = userId,
                                    toUserId = request.authorId,
                                    currency = Currency.RUB,
                                    amount = request.amount
                                ) as PaymentAsyncRequest
                            }
                        }
                    }

                    val response = subscriptionRepository.subscribeToAuthor(userId, request)
                    call.respond(response)
                }

                delete("/subscription") {
                    val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                    val subscriptionId = UUID.fromString(call.parameters["subscriptionId"])

                    call.respond(subscriptionRepository.cancelSubscription(subscriptionId, userId))
                }

                post("/support") {
                    val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                    val request = call.receive<SupportAuthorRequest>()
                    val balance = walletRepository.getUserBalance(userId).currentBalance
                    if (balance < request.amount) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Insufficient funds"))
                        return@post
                    }

                    rabbitmq {
                        basicPublish {
                            exchange = config.ktor.rabbitmq.exchange
                            routingKey = PAYMENT_ROUTING_KEY
                            properties = basicProperties {
                                correlationId = UUID.randomUUID().toString()
                                replyTo = config.ktor.jwt.audience
                            }
                            message {
                                PaymentAsyncRequest.TransferFundsRequest(
                                    fromUserId = userId,
                                    toUserId = request.authorId,
                                    currency = Currency.RUB,
                                    amount = request.amount
                                ) as PaymentAsyncRequest
                            }
                        }
                    }

                    val supportId = UUID.randomUUID().toString()
                    val now = java.time.LocalDateTime.now().toString()
                    val response = SupportAuthorResponse(
                        supportId = supportId,
                        userId = userId,
                        authorId = request.authorId,
                        amount = request.amount,
                        supportedAt = now
                    )
                    call.respond(response)
                }
            }
        }
    }
}