package data

import data.repositories.HiddenAuthorRepository
import data.repositories.PostRepository
import org.koin.dsl.module

internal fun dataModule() = module {
    single { PostRepository() }
    single { HiddenAuthorRepository() }
}