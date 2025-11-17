package com.example.routes

import clients.UsersServiceClient
import com.example.ClientConfig
import com.example.clients.SupportServiceClient
import com.example.commonPlugins.*
import com.example.config.ServiceConfig
import com.example.createServiceHttpClient
import com.example.module
import com.example.testServiceConfig
import com.example.utils.TimeUtils
import data.dataModule
import data.db.tables.HiddenAuthorsTable
import data.db.tables.PostsTable
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlinx.datetime.toDeprecatedInstant
import kotlinx.serialization.json.*
import org.koin.dsl.module
import posts.request.CreatePostRequest
import posts.request.UpdatePostRequest
import routes.configurePostsRouting
import support.TicketStatus
import support.response.TicketResponse
import users.UserRole
import kotlin.test.DefaultAsserter.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

private val testJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

private val jwtToken = JwtTokenService().generate(
    config = testServiceConfig(),
    TokenClaim(
        name = "id",
        value = "100"
    ),
    TokenClaim(
        name = "role",
        value = UserRole.USER.toString()
    )
)

private val httpClient = createServiceHttpClient(ClientConfig(baseUrl = "test"))

class MockUsersServiceClient :
    UsersServiceClient(httpClient) {

    override suspend fun followAuthor(authorId: Long, jwtToken: String): HttpStatusCode {
        return HttpStatusCode.OK
    }

    override suspend fun unfollowAuthor(authorId: Long, jwtToken: String): HttpStatusCode {
        return HttpStatusCode.OK
    }
}

class MockSupportServiceClient : SupportServiceClient(httpClient) {
    @OptIn(ExperimentalTime::class)
    override suspend fun complaint(
        subject: String,
        body: String,
        jwtToken: String
    ): TicketResponse {
        return TicketResponse(
            id = "",
            authorId = "",
            subject = subject,
            body = body,
            status = TicketStatus.OPEN,
            assigneeId = "",
            createdAt = TimeUtils.currentUtcOffsetDateTime().toInstant().toKotlinInstant().toDeprecatedInstant(),
            updatedAt = TimeUtils.currentUtcOffsetDateTime().toInstant().toKotlinInstant().toDeprecatedInstant(),
            closedAt = TimeUtils.currentUtcOffsetDateTime().toInstant().toKotlinInstant().toDeprecatedInstant()
        )
    }
}


internal fun testClientsModule() = module {
    single<UsersServiceClient> { MockUsersServiceClient() }
    single<SupportServiceClient> { MockSupportServiceClient() }
}

fun Application.testModule(config: ServiceConfig) {
    configureOpenApi()
    configureMonitoring()
    configureSerialization()
    configureKoin(
        otherModules = listOf(
            dataModule(),
            testClientsModule()
        ),
    )
//    val routing = "testing"
//    configureRabbitMQ(
//        config = config,
//        configuration = {
//            configureRabbitRouting(
//                application = this@module,
//                config = config,
//                routing = routing
//            )
//        },
//        routing = routing,
//    )

    configureSecurity(config)
    configureCommonRouting()
    DatabaseFactory.initializationDatabase(
        config = config,
        tables = arrayOf(
            PostsTable,
            HiddenAuthorsTable
        )
    )

    configurePostsRouting()
}

class PostsRoutingTest {

    @Test
    fun `creating a post returns persisted payload`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        val request = CreatePostRequest(
            title = "My New Post",
            content = "This is the content of my new post.",
            tags = listOf("tech", "blog")
        )
        val response = client.post("/api/v1/posts") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        CreatePostRequest.serializer(),
                        request
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }
        val createdRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for create post: $createdRaw")
        val post = runCatching {
            testJson.parseToJsonElement(createdRaw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse post JSON: $createdRaw", it) }
        assertEquals(100L, post["authorId"]?.jsonPrimitive?.longOrNull)
        assertEquals("My New Post", post["title"]?.jsonPrimitive?.content)
        assertEquals(listOf("tech", "blog"), post["tags"]?.jsonArray?.map { it.jsonPrimitive.content })
    }

    @Test
    fun `get post by id returns correct post`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        val createRequest = CreatePostRequest(
            title = "Post to Get",
            content = "Content to retrieve.",
            tags = emptyList()
        )
        val createResponse = client.post("/api/v1/posts") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        CreatePostRequest.serializer(),
                        createRequest
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }
        val createdPostId = runCatching {
            testJson.parseToJsonElement(createResponse.bodyAsText()).jsonObject["id"]?.jsonPrimitive?.content
        }.getOrNull()
        assertNotNull(createdPostId, "Failed to get post ID for get by id test")
        val response = client.get("/api/v1/posts") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("postId", createdPostId)
        }
        val getRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for get post by id: $getRaw")
        val post = runCatching {
            testJson.parseToJsonElement(getRaw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse retrieved post JSON: $getRaw", it) }
        assertEquals("Post to Get", post["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun `update post changes the post data`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        val createRequest = CreatePostRequest(
            title = "Old Title",
            content = "Old Content",
            tags = listOf()
        )
        val createResponse = client.post("/api/v1/posts") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        CreatePostRequest.serializer(),
                        createRequest
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }
        val createdPostId = runCatching {
            testJson.parseToJsonElement(createResponse.bodyAsText()).jsonObject["id"]?.jsonPrimitive?.content
        }.getOrNull()
        assertNotNull(createdPostId, "Failed to get post ID for update test")
        val updateRequest = UpdatePostRequest(
            title = "New Title",
            content = "New Content",
            tags = emptyList()
        )
        val response = client.patch("/api/v1/posts") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("postId", createdPostId)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        UpdatePostRequest.serializer(),
                        updateRequest
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }
        val updatedRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for update post: $updatedRaw")
        val post = runCatching {
            testJson.parseToJsonElement(updatedRaw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse updated post JSON: $updatedRaw", it) }
        assertEquals("New Title", post["title"]?.jsonPrimitive?.content)
        assertEquals("PUBLISHED", post["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun `delete post marks post as deleted`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        val createRequest = CreatePostRequest(
            title = "Post to Delete",
            content = "Content to delete.",
            tags = listOf()
        )
        val createResponse = client.post("/api/v1/posts") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        CreatePostRequest.serializer(),
                        createRequest
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }
        val createdPostId = runCatching {
            testJson.parseToJsonElement(createResponse.bodyAsText()).jsonObject["id"]?.jsonPrimitive?.content
        }.getOrNull()
        assertNotNull(createdPostId, "Failed to get post ID for delete test")
        val response = client.delete("/api/v1/posts") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("postId", createdPostId)
        }
        val deleteRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for delete post: $deleteRaw")
        val result = runCatching {
            testJson.parseToJsonElement(deleteRaw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse delete response JSON: $deleteRaw", it) }
        assertEquals(createdPostId, result["id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `search posts returns filtered results`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        val createRequest1 = CreatePostRequest(
            title = "Tech Post",
            content = "About technology",
            tags = listOf("tech")
        )
        val createRequest2 = CreatePostRequest(
            title = "Sports Post",
            content = "About sports",
            tags = listOf("sports")
        )
        client.post("/api/v1/posts") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        CreatePostRequest.serializer(),
                        createRequest1
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }.apply { assertEquals(HttpStatusCode.OK, status) }
        client.post("/api/v1/posts") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        CreatePostRequest.serializer(),
                        createRequest2
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }.apply { assertEquals(HttpStatusCode.OK, status) }
        val response = client.get("/api/v1/posts/search?query=tech") {
            header(HttpHeaders.Authorization, authHeader)
        }
        val searchRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for search posts: $searchRaw")
        val results = runCatching {
            testJson.parseToJsonElement(searchRaw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse search results JSON: $searchRaw", it) }
        val posts = results["posts"]?.jsonArray ?: emptyList()
        assertEquals(1, posts.size)
        assertEquals("Tech Post", posts.first().jsonObject["title"]?.jsonPrimitive?.content)
    }

    @Test
    fun `hiding an author persists the action`() = testApplication {
        application {
            testModule(testServiceConfig())
        }
        val authHeader = "Bearer $jwtToken"
        val response = client.post("/api/v1/posts/hidden") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("authorId", "200")
        }
        val resultRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for hide author: $resultRaw")
    }

    @Test
    fun `restoring an author persists the action`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        client.post("/api/v1/posts/hidden?authorId=200") {
            header(HttpHeaders.Authorization, authHeader)
        }.apply { assertEquals(HttpStatusCode.OK, status) }
        val response = client.delete("/api/v1/posts/hidden?authorId=200") {
            header(HttpHeaders.Authorization, authHeader)
        }
        val resultRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for restore author: $resultRaw")
    }

    @Test
    fun `follow author sends request to users service`() = testApplication {
        application { testModule(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        val response = client.post("/api/v1/posts/follow") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("authorId", "125")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `unfollow author sends request to users service`() = testApplication {
        application { testModule(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        client.post("/api/v1/posts/follow") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("authorId", "125")
        }
        val response = client.delete("/api/v1/posts/follow") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("authorId", "125")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `complaint on post sends request to support service`() = testApplication {
        application { testModule(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        val request = posts.request.ComplaintRequest(
            reason = "Inappropriate content"
        )
        val response = client.post("/api/v1/posts/complaint/post") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("postId", "6a8a1238-7d41-45b9-8ac5-a2c311e31fd5")
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        posts.request.ComplaintRequest.serializer(),
                        request
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `complaint on user sends request to support service`() = testApplication {
        application { testModule(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        val request = posts.request.ComplaintRequest(
            reason = "Spam"
        )
        val response = client.post("/api/v1/posts/complaint/user?userId=2") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        posts.request.ComplaintRequest.serializer(),
                        request
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
