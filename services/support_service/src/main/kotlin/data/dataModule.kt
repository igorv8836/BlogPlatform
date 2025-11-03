package com.example.data

import com.example.data.repositories.TicketRepository
import org.koin.dsl.module

fun dataModule() = module {
    single { TicketRepository() }
}