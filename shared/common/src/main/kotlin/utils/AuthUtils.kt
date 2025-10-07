package com.example.utils

import com.example.constants.BaseConstants
import com.example.constants.UnauthorizedException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun ApplicationCall.userIdOrNull(): String? =
    principal<JWTPrincipal>()?.payload?.subject

fun ApplicationCall.userId(): String {
    return principal<JWTPrincipal>()?.payload?.subject ?: throw UnauthorizedException()
}

suspend fun ApplicationCall.requireRole(role: String) {
    val roles = principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("roles")
        ?.asList(String::class.java)
        ?: emptyList()

    if (role !in roles) {
        respondText(status = HttpStatusCode.Forbidden, text = BaseConstants.FORBIDDEN.value)
    }
}