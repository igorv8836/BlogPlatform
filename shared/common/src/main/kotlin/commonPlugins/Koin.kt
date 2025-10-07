package com.example.commonPlugins

import com.example.commonModule
import io.ktor.server.application.*
import org.koin.core.module.Module
import org.koin.ktor.plugin.Koin

fun Application.configureKoin(
    otherModules: List<Module> = emptyList(),
) {
    install(Koin) {
        val commonModules: List<Module> = listOf(
            commonModule()
        )
        modules(modules = commonModules + otherModules)
    }
}