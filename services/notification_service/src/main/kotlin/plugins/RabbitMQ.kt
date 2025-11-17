package com.example.plugins

import com.example.clients.UsersServiceClient
import com.example.config.ServiceConfig
import com.example.data.repositories.NotificationRepository
import com.example.data.repositories.NotificationRepositoryImpl
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicAck
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicConsume
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.github.damir.denis.tudor.ktor.server.rabbitmq.rabbitMQ
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import notification.request.NotificationAsyncRequest
import org.koin.ktor.ext.inject

fun Routing.configureRabbitRouting(
    application: Application,
    config: ServiceConfig,
    routing: String
) {
    val notificationRepository: NotificationRepository = NotificationRepositoryImpl()
    val usersServiceClient by inject<UsersServiceClient>()

    rabbitmq {

        basicConsume {
            autoAck = false
            queue = config.ktor.rabbitmq.queue
            dispatcher = Dispatchers.rabbitMQ
            coroutinePollSize = 50

            deliverCallback<NotificationAsyncRequest> { message ->
                application.log.info("Requested information: ${message.body}")
                val replyTo = message.properties.replyTo

                application.log.info("ReplyTo: $replyTo")

                basicAck { deliveryTag = message.envelope.deliveryTag }

                    when (val req = message.body) {
                        is NotificationAsyncRequest.CreateNotificationRequest -> {
                            val userId = req.targetUserId
                                ?: throw IllegalArgumentException("targetUserId is null for non-broadcast request")

                            notificationRepository.create(
                                message = req.message,
                                type = req.notificationType,
                                userId = userId
                            )
                        }

                        is NotificationAsyncRequest.NotifySubscribersNotificationRequest -> {
                            val subscribers: List<Long> = usersServiceClient.getSubscribedUsers(req.authorUserId, req.token).followersId

                            subscribers.forEach { subscriberId ->
                                notificationRepository.create(
                                    message = req.message,
                                    type = req.notificationType,
                                    userId = subscriberId
                                )
                            }
                        }
                    }


            }

            deliverFailureCallback { message ->
                application.log.error(
                    "Notification async: deserialization failed: ${message.body.toString(Charsets.UTF_8)}"
                )
            }
        }
    }
}