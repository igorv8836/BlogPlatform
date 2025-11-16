package data

import clients.UsersServiceClient
import data.repositories.HiddenAuthorRepository
import data.repositories.PostRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal fun dataModule() = module {
    single { PostRepository() }
    single { HiddenAuthorRepository() }
    single { UsersServiceClient(get(named("users_client"))) }
}