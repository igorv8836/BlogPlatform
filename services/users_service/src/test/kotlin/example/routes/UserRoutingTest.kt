package com.example.routes

import com.example.ClientConfig
import com.example.clients.SupportServiceClient
import com.example.clients.WalletServiceClient
import com.example.commonPlugins.*
import com.example.config.ServiceConfig
import com.example.createServiceHttpClient
import com.example.data.dataModule
import com.example.data.db.tables.BanTable
import com.example.data.db.tables.FollowTable
import com.example.data.db.tables.UserTable
import com.example.plugins.configureRabbitRouting
import com.example.testServiceConfig
import com.example.utils.TimeUtils
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import junit.framework.TestCase.assertTrue
import kotlinx.datetime.toDeprecatedInstant
import kotlinx.serialization.json.*
import org.koin.dsl.module
import posts.request.ComplaintRequest
import support.TicketStatus
import support.response.TicketResponse
import users.UserRole
import users.request.AuthRequest
import users.request.BanRequest
import users.request.RecoveryRequest
import users.request.RegisterRequest
import users.request.UpdateProfileRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

private val httpClient = createServiceHttpClient(ClientConfig(baseUrl = "test"))

class MockWalletServiceClient : WalletServiceClient(httpClient) {
    override suspend fun createWallet(jwtToken: String): HttpStatusCode {
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
    single<WalletServiceClient> { MockWalletServiceClient() }
    single<SupportServiceClient> { MockSupportServiceClient() }
}

fun Application.testUserModule(config: ServiceConfig) {
    configureOpenApi()
    configureMonitoring()
    configureSerialization()
    configureKoin(
        otherModules = listOf(
            dataModule(),
            testClientsModule()
        )
    )

    val routing = config.ktor.jwt.audience
    configureRabbitMQ(
        config = config,
        configuration = {
            configureRabbitRouting(
                application = this@testUserModule,
                config = config,
                routing = routing
            )
        },
        routing = routing,
    )

    configureSecurity(config)
    configureCommonRouting()

    DatabaseFactory.initializationDatabase(
        config = config,
        tables = arrayOf(
            UserTable,
            FollowTable,
            BanTable
        )
    )

    userRouting(config)
}

class UserRoutingTest {

    private val json = Json { ignoreUnknownKeys = true }
    private fun tokenFor(id: Int, role: UserRole = UserRole.USER): String {
        return "Bearer " + JwtTokenService().generate(
            config = testServiceConfig(),
            TokenClaim("id", id.toString()),
            TokenClaim("role", role.toString())
        )
    }

    @Test
    fun `register user success`() = testApplication {
        application { testUserModule(testServiceConfig()) }

        val req = RegisterRequest(
            login = "user0",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )

        val response = client.post("/api/v1/register") {
            setBody(json.encodeToString(RegisterRequest.serializer(), req))
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(response.bodyAsText())
    }

    @Test
    fun `register user conflict`() = testApplication {
        application { testUserModule(testServiceConfig()) }

        val req = RegisterRequest(
            login = "user1",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }

        val response = client.post("/api/v1/register") {
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

/*
    @Test
    fun `login success`() = testApplication {
        application { testUserModule(testServiceConfig()) }
        val req = RegisterRequest(
            login = "user2",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }

        val resp = client.post("/api/v1/login") {
            setBody(json.encodeToString(AuthRequest("user2", "test")))
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, resp.status)
        assertTrue(resp.bodyAsText().contains("token"))
    }
*/


    @Test
    fun `login invalid password returns unauthorized`() = testApplication {
        application { testUserModule(testServiceConfig()) }
        val req = RegisterRequest(
            login = "user3",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }

        val resp = client.post("/api/v1/login") {
            setBody(json.encodeToString(AuthRequest("user3", "WRONG")))
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun `recovery success`() = testApplication {
        application { testUserModule(testServiceConfig()) }

        val req = RegisterRequest(
            login = "user4",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }

        val response = client.post("/api/v1/recovery") {
            setBody(json.encodeToString(RecoveryRequest("user4")))
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `profile get success`() = testApplication {
        application { testUserModule(testServiceConfig()) }

        val req = RegisterRequest(
            login = "user5",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }

        val response = client.get("/api/v1/profile?id=1") {
            header(HttpHeaders.Authorization, tokenFor(1))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `profile update success`() = testApplication {
        application { testUserModule(testServiceConfig()) }

        val req0 = RegisterRequest(
            login = "user6",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req0))
            contentType(ContentType.Application.Json)
        }

        val req = UpdateProfileRequest(
            desc = "New bio",
            avatar = "new.png",
            password = "pass"
        )

        val response = client.put("/api/v1/profile?id=1") {
            header(HttpHeaders.Authorization, tokenFor(1))
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `follow user success`() = testApplication {
        application { testUserModule(testServiceConfig()) }

        val req = RegisterRequest(
            login = "userA",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }
        val req1 = RegisterRequest(
            login = "userB",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req1))
            contentType(ContentType.Application.Json)
        }

        val response = client.post("/api/v1/follow?id=2") {
            header(HttpHeaders.Authorization, tokenFor(1))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }



    @Test
    fun `unfollow user success`() = testApplication {
        application { testUserModule(testServiceConfig()) }

        val req = RegisterRequest(
            login = "userC",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }
        val req1 = RegisterRequest(
            login = "userD",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req1))
            contentType(ContentType.Application.Json)
        }

        client.post("/api/v1/follow?id=2") {
            header(HttpHeaders.Authorization, tokenFor(1))
        }

        val resp = client.delete("/api/v1/follow?id=2") {
            header(HttpHeaders.Authorization, tokenFor(1))
        }

        assertEquals(HttpStatusCode.OK, resp.status)
    }

    @Test
    fun `get followers list success`() = testApplication {
        application { testUserModule(testServiceConfig()) }

        val req = RegisterRequest(
            login = "user",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }

        val resp = client.get("/api/v1/followers?id=1") {
            header(HttpHeaders.Authorization, tokenFor(1))
        }

        assertEquals(HttpStatusCode.OK, resp.status)
    }

    @Test
    fun `report user sends complaint request`() = testApplication {
        application { testUserModule(testServiceConfig()) }

        val req = RegisterRequest(
            login = "reporter",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }
        val req0 = RegisterRequest(
            login = "offender",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req0))
            contentType(ContentType.Application.Json)
        }

        val response = client.post("/api/v1/report?id=2") {
            header(HttpHeaders.Authorization, tokenFor(1))
            setBody(json.encodeToString(ComplaintRequest("Spam")))
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `ban user success`() = testApplication {
        application { testUserModule(testServiceConfig()) }

        val req0 = RegisterRequest(
            login = "moderator",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req0))
            contentType(ContentType.Application.Json)
        }
        val req1 = RegisterRequest(
            login = "victim",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req1))
            contentType(ContentType.Application.Json)
        }

        val req = BanRequest(duration = 3, message = "Rule violation")

        val response = client.post("/api/v1/ban?id=2") {
            header(HttpHeaders.Authorization, tokenFor(1))
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }



    @Test
    fun `unban user success`() = testApplication {
        application { testUserModule(testServiceConfig()) }

        val req0 = RegisterRequest(
            login = "moderator2",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req0))
            contentType(ContentType.Application.Json)
        }
        val req1 = RegisterRequest(
            login = "victim2",
            password = "test",
            email = "user@mail.com",
            desc = "",
            avatar = null
        )
        client.post("/api/v1/register") {
            setBody(json.encodeToString(req1))
            contentType(ContentType.Application.Json)
        }

        val req = BanRequest(duration = 3, message = "Rule violation")

        client.post("/api/v1/ban?id=2") {
            header(HttpHeaders.Authorization, tokenFor(1))
            setBody(json.encodeToString(req))
            contentType(ContentType.Application.Json)
        }

        val resp = client.delete("/api/v1/ban?id=2") {
            header(HttpHeaders.Authorization, tokenFor(1))
        }

        assertEquals(HttpStatusCode.OK, resp.status)
    }
}