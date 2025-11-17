package com.example

import org.koin.core.qualifier.named
import org.koin.dsl.module

// TODO Somehow union that with /docker/.env
private const val COMMENTS_SERVICE_PORT=8081
private const val SUPPORT_SERVICE_PORT=8082
private const val POSTS_SERVICE_PORT=8083
private const val WALLET_SERVICE_PORT=8084
private const val USER_SERVICE_PORT=8085
private const val NOTIFICATION_SERVICE_PORT=8086
private const val PAYMENT_SERVICE_PORT=8087

// TODO We don't have in .env something like this
private const val BASE_URL = "http://0.0.0.0:"

fun coreClientModule() = module {

    single { createServiceHttpClient(ClientConfig(baseUrl = "test")) }

    single(named("users_client")) {
        createServiceHttpClient(
            ClientConfig(
                baseUrl = BASE_URL + USER_SERVICE_PORT
            )
        )
    }

    single(named("support_client")) {
        createServiceHttpClient(
            ClientConfig(
                baseUrl = BASE_URL + SUPPORT_SERVICE_PORT
            )
        )
    }

    single(named("wallet_client")) {
        createServiceHttpClient(
            ClientConfig(
                baseUrl = BASE_URL + WALLET_SERVICE_PORT
            )
        )
    }
}