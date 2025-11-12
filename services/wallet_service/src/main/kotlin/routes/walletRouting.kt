package routes

import com.example.utils.userIdOrNull
import data.repositories.PaymentMethodRepository
import data.repositories.SubscriptionRepository
import data.repositories.WalletRepository
import data.repositories.WithdrawalRequestRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import wallet.request.SupportAuthorRequest
import wallet.response.SupportAuthorResponse
import wallet.request.AddPaymentMethodRequest
import wallet.request.RequestWithdrawalRequest
import wallet.request.SubscribeToAuthorRequest
import java.util.*


fun Application.configureWalletRouting() {
    val walletRepository by inject<WalletRepository>()
    val paymentMethodRepository by inject<PaymentMethodRepository>()
    val withdrawalRequestRepository by inject<WithdrawalRequestRepository>()
    val subscriptionRepository by inject<SubscriptionRepository>()

    routing {
        route("/api/v1/wallet") {

            post {
                val userId: Long = call.userIdOrNull()?.toLong() ?: 100
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

            post("/credit") {
                val amount: Double = call.parameters["amount"]?.toDouble()
                    ?: throw BadRequestException("Parameter amount is null")
                val userId: Long = call.parameters["userId"]?.toLong()
                    ?: throw BadRequestException("userId is null")

                call.respond(walletRepository.credit(userId, amount))
            }

            post("/debit") {
                val amount: Double = call.parameters["amount"]?.toDouble()
                    ?: throw BadRequestException("Parameter amount is null")
                val userId: Long = call.parameters["userId"]?.toLong()
                    ?: throw BadRequestException("userId is null")

                call.respond(walletRepository.debit(userId, amount))
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
                // В реальности тут должна быть проверка способа оплаты и списание средств
                call.respond(withdrawalRequestRepository.requestWithdrawal(userId, request))
            }

            post("/subscription") {
                val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                val request = call.receive<SubscribeToAuthorRequest>()
                // В реальности тут нужно проверить баланс, способ оплаты и произвести первый платёж
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
                // В реальности тут нужно проверить способ оплаты и произвести платёж
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