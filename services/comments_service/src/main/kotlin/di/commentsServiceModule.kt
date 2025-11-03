package com.example.di

import com.example.data.dataModule
import org.koin.dsl.module

fun commentsServiceModule() = module {
    includes(dataModule())
}