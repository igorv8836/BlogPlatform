package com.example.clients

import org.koin.core.qualifier.named
import org.koin.dsl.module

internal fun clientsModule() = module {
    single { WalletServiceClient(get(named("wallet_client"))) }
    single { SupportServiceClient(get(named("support_client"))) }
}