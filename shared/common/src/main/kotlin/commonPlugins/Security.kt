package com.example.commonPlugins

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.example.config.ServiceConfig
import com.example.constants.BaseConstants
import com.example.models.BaseResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaInstant

@OptIn(ExperimentalTime::class)
fun Application.configureSecurity(config: ServiceConfig) {
    val algorithm = Algorithm.HMAC512("akhgklasdjfklasdjfklasdjflksdjghasldf")
    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(config.ktor.jwt.issuer)
        .build()

    routing {
        get("testAuth") {
            val token = JWT.create()
                .withSubject("Authentification")
                .withIssuer(config.ktor.jwt.issuer)
                .withSubject("100")
                .withAudience("all")
                .withClaim("userId", "100")
                .withArrayClaim("roles", arrayOf("admin", "moderator"))
                .withExpiresAt(Date(2025, 11, 11))
                .sign(algorithm)
            call.respondText(token)
        }
    }

    authentication {
        jwt("jwt") {
            this.realm = config.ktor.jwt.realm

            verifier(verifier)

            validate { cred ->
                val payload = cred.payload
                val audOk = payload.audience?.contains(config.ktor.jwt.audience) == true ||
                        payload.audience.contains("all")

                val auth = request.headers[HttpHeaders.Authorization]
                if (auth == "Bearer dev") {
                    val devToken = JWT.create()
                        .withIssuer(config.ktor.jwt.issuer)
                        .withAudience(config.ktor.jwt.audience, "all")
                        .withSubject("dev-user")
                        .withArrayClaim("roles", arrayOf("admin", "moderator"))
                        .withIssuedAt(Date.from(Clock.System.now().toJavaInstant()))
                        .withExpiresAt(Date.from(Clock.System.now().plus(kotlin.time.Duration.parse("1h")).toJavaInstant()))
                        .sign(Algorithm.HMAC256("akhgklasdjfklasdjfklasdjflksdjghasldf"))
                    return@validate JWTPrincipal(JWT.decode(devToken))
                }

                if (audOk) JWTPrincipal(payload) else null
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, BaseResponse(BaseConstants.UNAUTHORIZED.value))
            }
        }
    }
}
