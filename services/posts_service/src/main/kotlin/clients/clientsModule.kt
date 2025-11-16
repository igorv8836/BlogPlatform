package com.example.clients

import clients.UsersServiceClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal fun clientsModule() = module {
    single { UsersServiceClient(get(named("users_client"))) }
    single { SupportServiceClient(get(named("support_client"))) }
}