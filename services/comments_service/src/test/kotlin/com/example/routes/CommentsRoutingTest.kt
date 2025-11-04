package com.example.routes

import com.example.module
import com.example.testServiceConfig
import comments.request.CreateCommentRequest
import comments.request.EditCommentRequest
import comments.request.HideCommentRequest
import comments.request.TargetType
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val testJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

class CommentsRoutingTest {

    @Test
    fun `creating a comment returns persisted payload`() = testApplication {
        application {
            module(testServiceConfig())
        }

        val authHeader = "Bearer ${client.get("/testAuth").bodyAsText()}"

        val response = client.post("/api/v1/comments") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        CreateCommentRequest.serializer(),
                        CreateCommentRequest(
                            targetType = TargetType.Post,
                            targetId = "post-777",
                            body = "Hello there"
                        )
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }

        val createdRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for create: $createdRaw")
        val comment = runCatching {
            testJson.parseToJsonElement(createdRaw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse comment JSON: $createdRaw", it) }
        assertEquals("100", comment["authorId"]?.jsonPrimitive?.content)
        assertEquals("Hello there", comment["body"]?.jsonPrimitive?.content)
    }

    @Test
    fun `patch updates comment body and mentions`() = testApplication {
        application {
            module(testServiceConfig())
        }
        val authHeader = "Bearer ${client.get("/testAuth").bodyAsText()}"
        val created = client.post("/api/v1/comments") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        CreateCommentRequest.serializer(),
                        CreateCommentRequest(
                            targetType = TargetType.Post,
                            targetId = "post-888",
                            body = "Original text",
                            mentions = listOf("user-a")
                        )
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }.let {
            val body = it.bodyAsText()
            assertEquals(HttpStatusCode.OK, it.status, "Unexpected status for create in patch test: $body")
            runCatching {
                testJson.parseToJsonElement(body).jsonObject
            }.getOrElse { err -> throw AssertionError("Failed to parse created comment: $body", err) }
        }
        val createdId = created["id"]?.jsonPrimitive?.content ?: error("missing comment id")

        val updated = client.patch("/api/v1/comments/$createdId") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        EditCommentRequest.serializer(),
                        EditCommentRequest(body = "Updated body", mentions = listOf("user-b"))
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }

        val raw = updated.bodyAsText()
        assertEquals(HttpStatusCode.OK, updated.status, "Unexpected status for patch: $raw")
        val payload = runCatching {
            testJson.parseToJsonElement(raw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse updated comment: $raw", it) }
        assertEquals("Updated body", payload["body"]?.jsonPrimitive?.content)
        assertTrue(payload["edited"]?.jsonPrimitive?.booleanOrNull == true)
        assertEquals(listOf("user-b"), payload["mentions"]?.jsonArray?.map { it.jsonPrimitive.content })
    }

    @Test
    fun `moderator can hide comment and list reflects change`() = testApplication {
        application {
            module(testServiceConfig())
        }
        val authHeader = "Bearer ${client.get("/testAuth").bodyAsText()}"
        val created = client.post("/api/v1/comments") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        CreateCommentRequest.serializer(),
                        CreateCommentRequest(
                            targetType = TargetType.Post,
                            targetId = "post-999",
                            body = "Hide me"
                        )
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }.let {
            val body = it.bodyAsText()
            assertEquals(HttpStatusCode.OK, it.status, "Unexpected status for create in hide test: $body")
            runCatching {
                testJson.parseToJsonElement(body).jsonObject
            }.getOrElse { err -> throw AssertionError("Failed to parse created comment: $body", err) }
        }
        val createdId = created["id"]?.jsonPrimitive?.content ?: error("missing comment id")

        val hideResponse = client.post("/api/v1/comments/$createdId/hide") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        HideCommentRequest.serializer(),
                        HideCommentRequest(reason = "spam")
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }
        val hideRaw = hideResponse.bodyAsText()
        assertEquals(HttpStatusCode.OK, hideResponse.status, "Unexpected status for hide: $hideRaw")

        val listResponse = client.get("/api/v1/comments") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("target_type", "post")
            parameter("target_id", "post-999")
        }
        val listRaw = listResponse.bodyAsText()
        assertEquals(HttpStatusCode.OK, listResponse.status, "Unexpected status for list: $listRaw")

        val comments = runCatching {
            testJson.parseToJsonElement(listRaw).jsonArray
        }.getOrElse { throw AssertionError("Failed to parse comment list: $listRaw", it) }
        assertTrue(comments.first().jsonObject["isHidden"]?.jsonPrimitive?.booleanOrNull == true)
    }
}
