package com.example.routes

import com.example.constants.UnauthorizedException
import com.example.data.repositories.NotificationRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import notification.NotificationType
import notification.request.NotificationRequest
import org.koin.ktor.ext.inject

fun Application.commentsRouting() {
    val notificationRepository: NotificationRepository by inject()

    routing {
        authenticate("jwt") {
            route("/api/v1") {
                route("/notification") {
                    post {
                        val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.NotFound)
                        val forBroadcast = call.parameters["forBroadcast"] ?: return@post call.respond(HttpStatusCode.NotFound)

                        val principal = call.principal<JWTPrincipal>()
                        val userLogin = principal?.getClaim("login", String::class) ?: throw UnauthorizedException()

                        val req = call.receive<NotificationRequest>()

                        if(forBroadcast.toBoolean())
                        {
                            val subscribedUsersId = listOf<Long>() //receive subscribe users id

                            for (subscribedUserId in subscribedUsersId)
                                notificationRepository.create(
                                    req.message,
                                    NotificationType.valueOf(req.type),
                                    subscribedUserId
                                )
                        }
                        else
                            notificationRepository.create(
                                req.message,
                                NotificationType.valueOf(req.type),
                                id.toLong()
                            )
                        call.respond(HttpStatusCode.OK)
                    }

                    get {
                        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.NotFound)

                        val principal = call.principal<JWTPrincipal>()
                        val userLogin = principal?.getClaim("login", String::class) ?: throw UnauthorizedException()

                        val response = notificationRepository.findById(
                            id.toLong()
                        )

                        if(response != null)
                            call.respond(response)
                        else
                            call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}
