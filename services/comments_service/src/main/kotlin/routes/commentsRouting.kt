package com.example.routes

import com.example.data.repositories.CommentRepository
import com.example.data.repositories.ComplaintRepository
import com.example.data.repositories.ReactionRepository
import com.example.models.Role
import com.example.utils.*
import comments.ReactionType
import comments.request.ComplaintRequest
import comments.request.CreateCommentRequest
import comments.request.EditCommentRequest
import comments.request.ReactionRequest
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.*

fun Application.commentsRouting() {
    val comments: CommentRepository by inject()
    val reactions: ReactionRepository by inject()
    val complaints: ComplaintRepository by inject()

    routing {
        authenticate("jwt") {
            route("/api/v1/comments") {
                post {
                    val userId = call.userId()
                    val req = call.receive<CreateCommentRequest>()
                    val created = comments.create(userId, req)
                    call.respondNotNull(created)
                }

                get {
                    val targetType = call.requireParameter("targetType")
                    val targetId = call.requireParameter("targetId")
                    val limit = call.requireParameter("limit").toIntOrNull() ?: 20
                    val offset = call.requireParameter("offset").toIntOrNull() ?: 0
                    call.respond(comments.list(targetType, targetId, limit, offset))
                }

                post("{id}/reply") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.parameters["id"])
                    val body = call.receive<EditCommentRequest>().body
                    call.respondNotNull(comments.reply(userId, id, body))
                }

                patch("{id}") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.parameters["id"])
                    val req = call.receive<EditCommentRequest>()
                    call.respondNotNull(comments.edit(id, userId, req.body))
                }

                delete("{id}") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.parameters["id"])
                    comments.softDelete(id, userId)
                    call.handleSuccess()
                }

                post("{id}/pin") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.parameters["id"])
                    comments.pin(id, userId)
                    call.handleSuccess()
                }

                delete("{id}/pin") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.parameters["id"])
                    comments.unpin(id, userId)
                    call.handleSuccess()
                }

                post("{id}/reactions") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.parameters["id"])
                    val req = call.receive<ReactionRequest>()
                    reactions.set(id, userId, req.type)
                    call.handleSuccess()
                }

                delete("{id}/reactions/{type}") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.parameters["id"])
                    val type = ReactionType.valueOf(call.parameters["type"]!!)
                    reactions.remove(id, userId, type)
                    call.handleSuccess()
                }

                post("{id}/complaints") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.parameters["id"])
                    val req = call.receive<ComplaintRequest>()
                    complaints.create(id, userId, req.reason)
                    call.handleSuccess()
                }

                post("{id}/hide") {
                    val moderatorId = call.userId()
                    call.requireRole(Role.Moderator)
                    val id = UUID.fromString(call.parameters["id"])
                    val reason = call.request.queryParameters["reason"]
                    comments.hide(id, moderatorId, reason)
                    call.handleSuccess()
                }

                post("{id}/restore") {
                    val moderatorId = call.userId()
                    call.requireRole(Role.Moderator)
                    val id = UUID.fromString(call.parameters["id"])
                    comments.restore(id, moderatorId)
                    call.handleSuccess()
                }
            }
        }
    }
}
