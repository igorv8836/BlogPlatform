package com.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun createServiceHttpClient(cfg: ClientConfig): HttpClient = HttpClient(CIO) {
    expectSuccess = false

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }
        )
    }

    install(Logging) { level = LogLevel.INFO }

    install(HttpTimeout) {
        connectTimeoutMillis = cfg.timeouts.connectMs
        requestTimeoutMillis = cfg.timeouts.requestMs
        socketTimeoutMillis  = cfg.timeouts.socketMs
    }

    install(HttpRequestRetry) {
        maxRetries = 3
        retryIf { _, resp -> resp.status.value >= 500 }
        retryOnExceptionIf { _, cause ->
            cause is HttpRequestTimeoutException || cause is ConnectTimeoutException
        }
        exponentialDelay()
    }

    defaultRequest {
        url(cfg.baseUrl)
        header(HttpHeaders.Accept, ContentType.Application.Json)
    }
}