package com.example.data

import com.example.data.repositories.CommentRepository
import com.example.data.repositories.ComplaintRepository
import com.example.data.repositories.ReactionRepository
import org.koin.dsl.module

internal fun dataModule() = module {
    single { CommentRepository() }
    single { ComplaintRepository() }
    single { ReactionRepository() }
}