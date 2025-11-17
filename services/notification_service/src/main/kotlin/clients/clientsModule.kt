package com.example.clients

import org.koin.core.qualifier.named
import org.koin.dsl.module

internal fun clientsModule() = module {
    single { UsersServiceClient(get(named("users_client"))) }
}