package com.example.clients

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.append

class WalletServiceClient(
    private val httpClient: HttpClient
) {
    suspend fun createWallet(
        jwtToken: String
    ): HttpStatusCode {
        try {
            val response: HttpResponse = httpClient.post("/api/v1/wallet") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $jwtToken")
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
            }
            return response.status

        } catch (e: Exception) {
            println(e)
            return HttpStatusCode.BadRequest.description(e.toString())
        }
    }
}