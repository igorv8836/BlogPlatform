package plugins

import com.example.config.ServiceConfig
import data.repositories.WalletRepository
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicAck
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicConsume
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.github.damir.denis.tudor.ktor.server.rabbitmq.rabbitMQ
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import wallet.request.WalletAsyncRequest

fun Routing.configureRabbitRouting(
    application: Application,
    config: ServiceConfig,
    routing: String
) {

    val walletRepository = WalletRepository()

    rabbitmq {
        basicConsume {
            autoAck = false
            queue = config.ktor.rabbitmq.queue
            dispatcher = Dispatchers.rabbitMQ
            coroutinePollSize = 100

            deliverCallback<WalletAsyncRequest> { message ->
                application.log.info("${message.body}")

                when(val walletAsyncRequest = message.body) {
                    is WalletAsyncRequest.CreditRequest -> {
                        walletRepository.credit(walletAsyncRequest.userId, walletAsyncRequest.creditedAmount)
                    }
                    is WalletAsyncRequest.DebitRequest -> {
                        walletRepository.debit(walletAsyncRequest.userId, walletAsyncRequest.debitedAmount)
                    }
                }

                basicAck { deliveryTag = message.envelope.deliveryTag }
            }

            deliverFailureCallback { message ->
                application.log.error("Received undeliverable message (deserialization failed): ${message.body.toString(Charsets.UTF_8)}")
            }
        }
    }
}