package com.example.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import support.request.CreateTicketRequest
import support.response.TicketResponse

open class SupportServiceClient(
    private val httpClient: HttpClient
) {
    open suspend fun complaint(
        subject: String,
        body: String,
        jwtToken: String
    ): TicketResponse {
        try {
            val response: HttpResponse = httpClient.post("/api/v1/tickets") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $jwtToken")
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                setBody(CreateTicketRequest(subject = subject, body = body))
            }
            return response.body() as TicketResponse

        } catch (_: Exception) {
            throw InternalError("Something is happened")
        }
    }
}