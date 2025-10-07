package com.example.commonPlugins

import com.auth0.jwk.JwkProviderBuilder
import com.example.config.ServiceConfig
import com.example.constants.BaseConstants
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.net.URL
import java.util.concurrent.TimeUnit

fun Application.configureSecurity(config: ServiceConfig) {
    val jwkProvider = JwkProviderBuilder(URL(config.ktor.jwt.jwksUrl))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt("auth-jwt") {
            this.realm = config.ktor.jwt.realm

            verifier(jwkProvider, config.ktor.jwt.issuer)

            validate { cred ->
                val payload = cred.payload
                val audOk = payload.audience?.contains(config.ktor.jwt.audience) == true ||
                        payload.audience.contains("all")

                if (audOk) JWTPrincipal(payload) else null
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, BaseConstants.UNAUTHORIZED.value)
            }
        }
    }
}
