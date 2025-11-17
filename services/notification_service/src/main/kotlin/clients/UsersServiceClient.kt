package com.example.clients

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import users.response.FollowersResponse


class UsersServiceClient(
    private val httpClient: HttpClient
) {
    suspend fun getSubscribedUsers(
        authorId: Long,
        jwtToken: String
    ): FollowersResponse {
        try {
            val response = httpClient.get("/api/v1/followers") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $jwtToken")
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                url {
                    parameters.append("id", "$authorId")
                }
            }
            return response.body() as FollowersResponse
        } catch (_: Exception) {
            throw InternalError("Something is happened")
        }
    }
}