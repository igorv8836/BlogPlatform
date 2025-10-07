package com.example.utils

import com.example.constants.AnswerType
import com.example.constants.ErrorType
import com.example.models.BaseResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

suspend fun ApplicationCall.handleSuccess(message: String? = null) {
    val answer = message ?: AnswerType.SUCCESS.message
    this.respond(HttpStatusCode.OK, BaseResponse(answer))
}

suspend fun ApplicationCall.handleUnauthorized(
    message: String? = null,
) {
    this.respond(
        HttpStatusCode.Unauthorized,
        BaseResponse(message ?: ErrorType.UNAUTHORIZED.message),
    )
}

suspend fun ApplicationCall.handleForbidden(
    message: String? = null,
) {
    this.respond(
        HttpStatusCode.Forbidden,
        BaseResponse(message ?: ErrorType.FORBIDDEN.message)
    )
}

suspend fun ApplicationCall.handleBadRequest(message: String = ErrorType.GENERAL.message) {
    this.respond(HttpStatusCode.BadRequest, BaseResponse(message))
}

suspend fun ApplicationCall.handleConflict(exception: Throwable) {
    this.respond(
        HttpStatusCode.Conflict,
        BaseResponse(exception.message ?: ErrorType.GENERAL.message)
    )
}
