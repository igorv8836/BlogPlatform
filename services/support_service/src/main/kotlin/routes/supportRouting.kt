package com.example.routes

import com.example.constants.Constants
import com.example.data.repositories.TicketRepository
import com.example.models.Role
import com.example.utils.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import support.request.*
import java.util.*

object SupportRoutes {
    const val AUTH = "jwt"
    const val BASE = "/api/v1/tickets"
}

fun Application.supportRouting() {
    val repo: TicketRepository by inject()

    routing {
        authenticate(SupportRoutes.AUTH) {
            route(SupportRoutes.BASE) {
                post {
                    val userId = call.userId()
                    val req = call.bodyOrException<CreateTicketRequest>()
                    call.respondNotNull(repo.create(userId, req))
                }

                get("/list") {
                    val req = call.bodyOrException<TicketListRequest>()
                    call.respond(repo.list(req))
                }

                get("{id}/messages") {
                    val id = UUID.fromString(call.requireParameter(Constants.ID))
                    call.respond(repo.messages(id))
                }

                post("/assign") {
                    call.requireRole(Role.Moderator)
                    val moderatorId = call.userId()
                    val req = call.bodyOrException<AssignRequest>()
                    call.respondNotNull(repo.assignSystem(req, moderatorId))
                }

                post("/clarify") {
                    call.requireRole(Role.Moderator)
                    val moderatorId = call.userId()
                    val req = call.bodyOrException<ClarifyRequest>()
                    call.respondNotNull(repo.requestClarification(req, moderatorId))
                }

                post("/moderate") {
                    call.requireRole(Role.Moderator)
                    val moderatorId = call.userId()
                    val req = call.bodyOrException<ModerateRequest>()
                    call.respondNotNull(repo.applyModeration(req, moderatorId))
                }

                post("/answer") {
                    call.requireRole(Role.Moderator)
                    val moderatorId = call.userId()
                    val req = call.bodyOrException<AnswerRequest>()
                    call.respondNotNull(repo.answer(req, moderatorId))
                }

                post("/close") {
                    val actorId = call.userId()
                    val req = call.bodyOrException<CloseRequest>()
                    call.respondNotNull(repo.close(req, actorId))
                }

                post("/rate") {
                    val userId = call.userId()
                    val req = call.bodyOrException<RateRequest>()
                    repo.rate(req, userId)
                    call.handleSuccess()
                }
            }
        }
    }
}
