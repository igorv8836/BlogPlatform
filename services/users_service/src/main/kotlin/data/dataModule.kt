package com.example.data

import com.example.data.repositories.BanRepository
import com.example.data.repositories.BanRepositoryImpl
import com.example.data.repositories.FollowRepository
import com.example.data.repositories.FollowRepositoryImpl
import com.example.data.repositories.UserRepository
import com.example.data.repositories.UserRepositoryImpl
import com.example.hashing.HashingService
import com.example.hashing.SHA256HashingService
import org.koin.dsl.bind
import org.koin.dsl.module
import com.example.security.JwtTokenService
import com.example.security.TokenService

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
        SHA256HashingService()
    } bind HashingService::class
    single {
        JwtTokenService()
    } bind TokenService::class
}