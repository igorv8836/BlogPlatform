package com.example.data

import com.example.data.repositories.NotificationRepository
import com.example.data.repositories.NotificationRepositoryImpl
import org.koin.dsl.bind
import org.koin.dsl.module

fun dataModule() = module {
    single {
        NotificationRepositoryImpl()
    } bind NotificationRepository::class
}