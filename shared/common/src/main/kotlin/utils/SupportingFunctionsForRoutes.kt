package com.example.utils

import com.example.constants.AnswerType
import com.example.constants.ConflictException
import com.example.constants.ErrorType
import com.example.constants.IncorrectQueryParameterException
import com.example.models.BaseResponse
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingCall.respondNotNull(response: Any?) {
    response?.let {
        this.respond(it)
    } ?: run {
        throw ConflictException(ErrorType.NULL_RESPONSE.message)
    }
}

fun RoutingCall.requireParameter(parameter: String): String {
    request.queryParameters[parameter]?.let {
        return it
    }
    throw IncorrectQueryParameterException("require parameter $parameter")
}

suspend fun RoutingCall.handleSuccess(message: String? = null) {
    val answer = message ?: AnswerType.SUCCESS.message
    this.respond(HttpStatusCode.OK, BaseResponse(answer))
}

suspend fun RoutingCall.handleUnauthorized(
    message: String? = null,
) {
    this.respond(
        HttpStatusCode.Unauthorized,
        BaseResponse(message ?: ErrorType.UNAUTHORIZED.message),
    )
}

suspend fun RoutingCall.handleForbidden(
    message: String? = null,
) {
    this.respond(
        HttpStatusCode.Forbidden,
        BaseResponse(message ?: ErrorType.FORBIDDEN.message)
    )
}

suspend fun RoutingCall.handleBadRequest(message: String = ErrorType.GENERAL.message) {
    this.respond(HttpStatusCode.BadRequest, BaseResponse(message))
}

suspend fun RoutingCall.handleConflict(exception: Throwable) {
    this.respond(
        HttpStatusCode.Conflict,
        BaseResponse(exception.message ?: ErrorType.GENERAL.message)
    )
}
