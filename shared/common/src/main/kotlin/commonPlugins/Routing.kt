package com.example.commonPlugins

import com.example.constants.*
import com.example.models.BaseResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCommonRouting() {
    install(StatusPages) {
        exception<UnauthorizedException> { call, cause ->
            call.respond(
                status = HttpStatusCode.Unauthorized,
                message = BaseResponse(cause.message ?: ErrorType.UNAUTHORIZED.message)
            )
        }

        exception<ForbiddenException> { call, cause ->
            call.respond(
                status = HttpStatusCode.Forbidden,
                message = BaseResponse(cause.message ?: ErrorType.FORBIDDEN.message)
            )
        }

        exception<BadRequestException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = BaseResponse(cause.message ?: ErrorType.INCORRECT_BODY.message)
            )
        }

        exception<IncorrectQueryParameterException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = BaseResponse(cause.message ?: ErrorType.INCORRECT_BODY.message)
            )
        }

        exception<ConflictException> { call, cause ->
            call.respond(
                status = HttpStatusCode.Conflict,
                message = BaseResponse(cause.message ?: ErrorType.GENERAL.message)
            )
        }

        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }

    }
    install(AutoHeadResponse)
    routing {
        get("/ping") {
            call.respondText("Hello World!")
        }
        staticResources("/static", "static")
    }
}
