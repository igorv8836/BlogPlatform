package com.example

import org.koin.dsl.module

fun coreClientModule() = module {
    single { createServiceHttpClient(ServiceConfig(baseUrl = "test")) }
}