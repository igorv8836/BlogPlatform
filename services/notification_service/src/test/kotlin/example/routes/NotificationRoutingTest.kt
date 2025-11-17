package com.example.routes

import com.example.ClientConfig
import com.example.clients.UsersServiceClient
import com.example.commonPlugins.DatabaseFactory
import com.example.commonPlugins.JwtTokenService
import com.example.commonPlugins.TokenClaim
import com.example.commonPlugins.configureKoin
import com.example.commonPlugins.configureMonitoring
import com.example.commonPlugins.configureOpenApi
import com.example.commonPlugins.configureSecurity
import com.example.commonPlugins.configureSerialization
import com.example.config.ServiceConfig
import com.example.createServiceHttpClient
import com.example.data.db.tables.NotificationTable
import com.example.data.repositories.NotificationRepository
import com.example.data.repositories.NotificationRepositoryImpl
import com.example.module
import com.example.routes.notificationRouting
import example.testServiceConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.Application
import io.ktor.server.testing.*
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.json.*
import notification.NotificationType
import notification.request.NotificationAsyncRequest
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import org.koin.ktor.plugin.Koin
import users.UserRole
import wallet.PaymentType
import wallet.SubscriptionStatus
import wallet.request.AddPaymentMethodRequest
import wallet.request.RequestWithdrawalRequest
import wallet.request.SubscribeToAuthorRequest
import wallet.request.SupportAuthorRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private val httpClient = createServiceHttpClient(ClientConfig(baseUrl = "test"))

private val testJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

private val jwtToken = JwtTokenService().generate(
    config = testServiceConfig(),
    TokenClaim(name = "id", value = "100"),
    TokenClaim(name = "role", value = UserRole.USER.toString())
)

internal fun testNotificationModule() = module {
    single<NotificationRepository> { NotificationRepositoryImpl() }
}

fun Application.testNotificationModule(config: ServiceConfig) {
    configureOpenApi()
    configureMonitoring()
    configureSerialization()
    configureKoin(
        otherModules = listOf(
            testNotificationModule()
        ),
    )

    configureSecurity(config)
    notificationRouting()
    DatabaseFactory.initializationDatabase(
        config = config,
        tables = arrayOf(
            NotificationTable
        )
    )
}

class NotificationRoutingTest {

    @Test
    fun `get notifications returns 404 when id missing`() = testApplication {
        application { testNotificationModule(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"

        val response = client.get("/api/v1/notification") {
            header(HttpHeaders.Authorization, authHeader)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `get notifications returns 401 when JWT invalid`() = testApplication {
        application { testNotificationModule(testServiceConfig()) }

        val response = client.get("/api/v1/notification?id=100") {
            header(HttpHeaders.Authorization, "Bearer invalid.token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

}