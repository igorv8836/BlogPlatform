package com.example.routes

import com.example.constants.UnauthorizedException
import com.example.data.repositories.NotificationRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Application.notificationRouting() {
    val notificationRepository: NotificationRepository by inject()

    routing {
        route("/api/v1") {
            authenticate("jwt") {
                route("/notification") {
                    get {
                        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.NotFound)

                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.getClaim("id", String::class) ?: throw UnauthorizedException()

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
