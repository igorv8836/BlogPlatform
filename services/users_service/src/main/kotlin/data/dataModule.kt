package data

import data.repositories.BanRepository
import data.repositories.BanRepositoryImpl
import data.repositories.FollowRepository
import data.repositories.FollowRepositoryImpl
import data.repositories.ReportRepository
import data.repositories.ReportRepositoryImpl
import data.repositories.UserRepository
import data.repositories.UserRepositoryImpl
import hashing.HashingService
import hashing.SHA256HashingService
import org.koin.dsl.bind
import org.koin.dsl.module
import security.JwtTokenService
import security.TokenService

fun dataModule() = module {
    single {
        UserRepositoryImpl()
    } bind UserRepository::class
    single {
        BanRepositoryImpl(get())
    } bind BanRepository::class
    single {
        FollowRepositoryImpl(get())
    } bind FollowRepository::class
    single {
        ReportRepositoryImpl(get())
    } bind ReportRepository::class
    single {
        SHA256HashingService()
    } bind HashingService::class
    single {
        JwtTokenService()
    } bind TokenService::class
}