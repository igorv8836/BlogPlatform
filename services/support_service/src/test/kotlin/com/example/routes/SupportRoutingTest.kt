package com.example.routes

import com.example.module
import com.example.testServiceConfig
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import support.TicketStatus
import support.request.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val testJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

class SupportRoutingTest {

    @Test
    fun `support ticket workflow succeeds via http endpoints`() = testApplication {
        application {
            module(testServiceConfig())
        }

        val authHeader = "Bearer ${client.get("/testAuth").bodyAsText()}"

        val createdResponse = client.post("/api/v1/tickets") {
            header(HttpHeaders.Authorization, authHeader)
            contentType(ContentType.Application.Json)
            setBody(
                testJson.encodeToString(
                    CreateTicketRequest.serializer(),
                    CreateTicketRequest(subject = "Broken post", body = "Please fix")
                )
            )
        }
        assertEquals(HttpStatusCode.OK, createdResponse.status)
        val createdRaw = createdResponse.bodyAsText()
        val createdTicket = runCatching { testJson.parseToJsonElement(createdRaw).jsonObject }
            .getOrElse { throw AssertionError("Failed to parse ticket: $createdRaw", it) }
        assertEquals(TicketStatus.OPEN.name, createdTicket["status"]?.jsonPrimitive?.content)
        val ticketId = createdTicket["id"]?.jsonPrimitive?.content ?: error("missing ticket id")

        val assignedResponse = client.post("/api/v1/tickets/assign") {
            header(HttpHeaders.Authorization, authHeader)
            contentType(ContentType.Application.Json)
            setBody(
                testJson.encodeToString(
                    AssignRequest.serializer(),
                    AssignRequest(ticketId = ticketId, assigneeId = "moderator-1")
                )
            )
        }
        val assignedRaw = assignedResponse.bodyAsText()
        val assignedTicket = testJson.parseToJsonElement(assignedRaw).jsonObject
        assertEquals(TicketStatus.IN_PROGRESS.name, assignedTicket["status"]?.jsonPrimitive?.content)
        assertEquals("moderator-1", assignedTicket["assigneeId"]?.jsonPrimitive?.content)

        val answeredResponse = client.post("/api/v1/tickets/answer") {
            header(HttpHeaders.Authorization, authHeader)
            contentType(ContentType.Application.Json)
            setBody(
                testJson.encodeToString(
                    AnswerRequest.serializer(),
                    AnswerRequest(ticketId = ticketId, message = "We are on it")
                )
            )
        }
        assertEquals(HttpStatusCode.OK, answeredResponse.status)
        val answeredTicket = testJson.parseToJsonElement(answeredResponse.bodyAsText()).jsonObject
        assertEquals(TicketStatus.ANSWERED.name, answeredTicket["status"]?.jsonPrimitive?.content)

        val closedResponse = client.post("/api/v1/tickets/close") {
            header(HttpHeaders.Authorization, authHeader)
            contentType(ContentType.Application.Json)
            setBody(
                testJson.encodeToString(
                    CloseRequest.serializer(),
                    CloseRequest(ticketId = ticketId)
                )
            )
        }
        val closedTicket = testJson.parseToJsonElement(closedResponse.bodyAsText()).jsonObject
        assertEquals(TicketStatus.CLOSED.name, closedTicket["status"]?.jsonPrimitive?.content)
        assertNotNull(closedTicket["closedAt"])

        val rateResponse = client.post("/api/v1/tickets/rate") {
            header(HttpHeaders.Authorization, authHeader)
            contentType(ContentType.Application.Json)
            setBody(
                testJson.encodeToString(
                    RateRequest.serializer(),
                    RateRequest(ticketId = ticketId, score = 5, comment = "Thanks")
                )
            )
        }
        assertEquals(HttpStatusCode.OK, rateResponse.status)

        val listResponse = client.request("/api/v1/tickets/list") {
            header(HttpHeaders.Authorization, authHeader)
            method = HttpMethod.Get
            contentType(ContentType.Application.Json)
            setBody(
                testJson.encodeToString(
                    TicketListRequest.serializer(),
                    TicketListRequest(status = TicketStatus.CLOSED)
                )
            )
        }
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val tickets = testJson.parseToJsonElement(listResponse.bodyAsText()).jsonArray
        assertTrue(tickets.any {
            val ticket = it.jsonObject
            ticket["id"]?.jsonPrimitive?.content == ticketId &&
                ticket["status"]?.jsonPrimitive?.content == TicketStatus.CLOSED.name
        })
    }
}
