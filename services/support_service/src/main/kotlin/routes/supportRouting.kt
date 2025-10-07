package com.example.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Application.supportRouting() {

    routing {
        authenticate("jwt") {
            route("/api/v1/support") {

            }
        }
    }
}
