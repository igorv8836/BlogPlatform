package org.example.data

import org.example.data.repositories.ComplaintRepository
import org.example.data.repositories.HiddenAuthorRepository
import org.example.data.repositories.PostRepository
import org.koin.dsl.module

internal fun dataModule() = module {
    single { PostRepository() }
    single { ComplaintRepository() }
    single { HiddenAuthorRepository() }
}