package com.example.utils

import com.example.constants.UnauthorizedException
import com.example.models.Role
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*

fun RoutingCall.userIdOrNull(): String? =
    principal<JWTPrincipal>()?.payload?.subject

fun RoutingCall.userId(): String {
    return principal<JWTPrincipal>()?.payload?.subject ?: throw UnauthorizedException()
}

suspend fun RoutingCall.requireRole(role: Role) {
    val roles = principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("roles")
        ?.asList(String::class.java)
        ?: emptyList()

    if (role.value !in roles) {
        return handleForbidden()
    }
}