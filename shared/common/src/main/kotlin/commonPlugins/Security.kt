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
import kotlin.time.ExperimentalTime

data class TokenClaim(
    val name: String,
    val value: String,
)

interface TokenService {
    fun generate(
        config: ServiceConfig,
        vararg tokenClaim: TokenClaim
    ): String
}

class JwtTokenService: TokenService {
    override fun generate(
        config: ServiceConfig,
        vararg tokenClaim: TokenClaim
    ): String {
        var token = JWT.create()
            .withAudience(config.ktor.jwt.audience)
            .withIssuer(config.ktor.jwt.issuer)
            .withExpiresAt(Date(System.currentTimeMillis() + config.ktor.jwt.expirationTime))

        tokenClaim.forEach { claim ->
            println(claim)
            token = token.withClaim(
                claim.name,
                claim.value
            )
        }

        return token.sign(Algorithm.HMAC256(config.ktor.jwt.secretKey))
    }

}

@OptIn(ExperimentalTime::class)
fun Application.configureSecurity(config: ServiceConfig) {
    val algorithm = Algorithm.HMAC256(config.ktor.jwt.secretKey)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withAudience(config.ktor.jwt.audience)
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

               /* if (auth == "Bearer dev") {
                    val devToken = JWT.create()
                        .withIssuer(config.ktor.jwt.issuer)
                        .withAudience(config.ktor.jwt.audience, "all")
                        .withSubject("dev-user")
                        .withArrayClaim("roles", arrayOf("user", "admin", "moderator"))
                        .withIssuedAt(Date.from(Clock.System.now().toJavaInstant()))
                        .withExpiresAt(Date.from(Clock.System.now().plus(kotlin.time.Duration.parse("1h")).toJavaInstant()))
                        .sign(Algorithm.HMAC256("akhgklasdjfklasdjfklasdjflksdjghasldf"))
                    return@validate JWTPrincipal(JWT.decode(devToken))
                }*/

                if (audOk) JWTPrincipal(payload) else null
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, BaseResponse(BaseConstants.UNAUTHORIZED.value))
            }
        }
    }
}
