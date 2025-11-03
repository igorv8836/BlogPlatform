package com.example.routes

import com.example.ServiceConstants
import com.example.constants.Constants
import com.example.constants.IncorrectQueryParameterException
import com.example.data.repositories.CommentRepository
import com.example.data.repositories.ComplaintRepository
import com.example.data.repositories.ReactionRepository
import com.example.models.Role
import com.example.utils.*
import comments.ReactionType
import comments.request.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
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
                    val req = call.bodyOrException<CreateCommentRequest>()
                    val created = comments.create(userId, req)

                    call.respondNotNull(created)
                }

                get {
                    val targetType = parseTargetType(call.requireQueryParameter(ServiceConstants.TARGET_TYPE))
                    val targetId = call.requireQueryParameter(ServiceConstants.TARGET_ID)
                    val limit = call.queryParameterOrNull(Constants.LIMIT)?.toIntOrNull() ?: 20
                    val offset = call.queryParameterOrNull(Constants.OFFSET)?.toIntOrNull() ?: 0

                    call.respond(comments.list(targetType, targetId, limit, offset))
                }
//
//                post("{${Constants.ID}}/reply") {
//                    val userId = call.userId()
//                    val id = UUID.fromString(call.requireParameter(Constants.ID))
//                    val req = call.bodyOrException<EditCommentRequest>()
//
//                    call.respondNotNull(comments.reply(userId, id, req))
//                }

                patch("{${Constants.ID}}") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.requireParameter(Constants.ID))
                    val req = call.bodyOrException<EditCommentRequest>()

                    call.respondNotNull(comments.edit(id, userId, req))
                }

                delete("{${Constants.ID}}") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.requireParameter(Constants.ID))

                    comments.softDelete(id, userId)
                    call.handleSuccess()
                }

                post("{${Constants.ID}}/pin") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.requireParameter(Constants.ID))

                    comments.pin(id, userId)
                    call.handleSuccess()
                }

                delete("{${Constants.ID}}/pin") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.requireParameter(Constants.ID))

                    comments.unpin(id, userId)
                    call.handleSuccess()
                }

                post("{${Constants.ID}}/complaints") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.requireParameter(Constants.ID))
                    val req = call.bodyOrException<ComplaintRequest>()

                    complaints.create(id, userId, req.reason)
                    call.handleSuccess()
                }

                post("{${Constants.ID}}/hide") {
                    val moderatorId = call.userId()
                    call.requireRole(Role.Moderator)
                    val id = UUID.fromString(call.requireParameter(Constants.ID))
                    val body = call.bodyOrException<HideCommentRequest>()

                    comments.hide(id, moderatorId, body.reason)
                    call.handleSuccess()
                }

                post("{${Constants.ID}}/restore") {
                    val moderatorId = call.userId()
                    call.requireRole(Role.Moderator)

                    val id = UUID.fromString(call.requireParameter(Constants.ID))
                    comments.restore(id, moderatorId)
                    call.handleSuccess()
                }

                post("{${Constants.ID}}/reactions/{${ServiceConstants.REACTION_TYPE}}") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.requireParameter(Constants.ID))
                    val type = parseReactionType(call.requireParameter(ServiceConstants.REACTION_TYPE))

                    reactions.set(id, userId, type)
                    call.handleSuccess()
                }

                delete("{${Constants.ID}}/reactions/{${ServiceConstants.REACTION_TYPE}}") {
                    val userId = call.userId()
                    val id = UUID.fromString(call.requireParameter(Constants.ID))
                    val type = parseReactionType(call.requireParameter(ServiceConstants.REACTION_TYPE))

                    reactions.remove(id, userId, type)
                    call.handleSuccess()
                }
            }
        }
    }
}

private fun parseTargetType(rawValue: String): TargetType =
    TargetType.entries.firstOrNull {
        it.value.equals(rawValue, ignoreCase = true) || it.name.equals(rawValue, ignoreCase = true)
    } ?: throw IncorrectQueryParameterException("Unknown target type: $rawValue")

private fun parseReactionType(rawValue: String): ReactionType =
    ReactionType.entries.firstOrNull { it.name.equals(rawValue, ignoreCase = true) }
        ?: throw IncorrectQueryParameterException("Unknown reaction type: $rawValue")
