package com.example.routes

import com.example.commonPlugins.JwtTokenService
import com.example.commonPlugins.TokenClaim
import com.example.module
import com.example.testServiceConfig
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
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

private suspend fun addPaymentMethodAndGetIt(client: HttpClient, authHeader: String): JsonObject {
    val request = AddPaymentMethodRequest(
        type = PaymentType.CARD,
        details = mapOf(
            "number" to "4111111111111111",
            "expiry" to "12/27",
            "cvv" to "123"
        )
    )
    val response = client.post("/api/v1/wallet/payment-method") {
        header(HttpHeaders.Authorization, authHeader)
        setBody(
            TextContent(
                text = testJson.encodeToString(
                    AddPaymentMethodRequest.serializer(),
                    request
                ),
                contentType = ContentType.Application.Json
            )
        )
    }
    val createdRaw = response.bodyAsText()
    assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for add payment method: $createdRaw")
    val method = runCatching {
        testJson.parseToJsonElement(createdRaw).jsonObject
    }.getOrElse { throw AssertionError("Failed to parse payment method JSON: $createdRaw", it) }

    return method
}

class WalletRoutingTest {

    @Test
    fun `creating a wallet returns persisted payload`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        val response = client.post("/api/v1/wallet") {
            header(HttpHeaders.Authorization, authHeader)
        }
        val createdRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for create wallet: $createdRaw")
        val wallet = runCatching {
            testJson.parseToJsonElement(createdRaw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse wallet JSON: $createdRaw", it) }
        assertEquals(100L, wallet["userId"]?.jsonPrimitive?.longOrNull)
        assertEquals(0.0, wallet["balance"]?.jsonPrimitive?.doubleOrNull)
    }

    @Test
    fun `get balance returns correct balance`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        client.post("/api/v1/wallet") {
            header(HttpHeaders.Authorization, authHeader)
        }.apply { assertEquals(HttpStatusCode.OK, status) }
        val response = client.get("/api/v1/wallet/balance") {
            header(HttpHeaders.Authorization, authHeader)
        }
        val balanceRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for get balance: $balanceRaw")
        val balance = runCatching {
            testJson.parseToJsonElement(balanceRaw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse balance JSON: $balanceRaw", it) }
        assertEquals(0.0, balance["currentBalance"]?.jsonPrimitive?.doubleOrNull)
    }

    @Test
    fun `adding a payment method returns persisted payload`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"

        val method = addPaymentMethodAndGetIt(client, authHeader)

        assertEquals(100L, method["userId"]?.jsonPrimitive?.longOrNull)
        assertEquals("****1111", method["maskedDetails"]?.jsonPrimitive?.content)
    }

    @Test
    fun `deleting a payment method`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"

        val method = addPaymentMethodAndGetIt(client, authHeader)

        val response = client.delete("/api/v1/wallet/payment-method") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("methodId", "${method["paymentMethodId"]?.jsonPrimitive?.content}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `requesting a withdrawal sends message and returns request`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        client.post("/api/v1/wallet") {
            header(HttpHeaders.Authorization, authHeader)
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        val method = addPaymentMethodAndGetIt(client, authHeader)

        val request = RequestWithdrawalRequest(
            amount = 0.0,
            currency = wallet.Currency.RUB,
            paymentMethodId = method["paymentMethodId"]?.jsonPrimitive?.content
                ?: throw NullPointerException("paymentId is null")
        )

        val response = client.post("/api/v1/wallet/withdrawal") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        RequestWithdrawalRequest.serializer(),
                        request
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }
        val createdRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for request withdrawal: $createdRaw")
        val withdrawal = runCatching {
            testJson.parseToJsonElement(createdRaw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse withdrawal request JSON: $createdRaw", it) }
        assertEquals(100L, withdrawal["userId"]?.jsonPrimitive?.longOrNull)
        assertEquals(0.0, withdrawal["amount"]?.jsonPrimitive?.doubleOrNull)
    }

    @Test
    fun `subscribing to an author sends transfer message and returns subscription`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        client.post("/api/v1/wallet") {
            header(HttpHeaders.Authorization, authHeader)
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        val method = addPaymentMethodAndGetIt(client, authHeader)

        val request = SubscribeToAuthorRequest(
            authorId = 200L,
            amount = 50.0,
            method["paymentMethodId"]?.jsonPrimitive?.content
                ?: throw NullPointerException("paymentId is null")
        )

        val response = client.post("/api/v1/wallet/subscription") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        SubscribeToAuthorRequest.serializer(),
                        request
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }
        val createdRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for subscribe: $createdRaw")
        val subscription = runCatching {
            testJson.parseToJsonElement(createdRaw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse subscription JSON: $createdRaw", it) }
        assertEquals(100L, subscription["userId"]?.jsonPrimitive?.longOrNull)
        assertEquals(200L, subscription["authorId"]?.jsonPrimitive?.longOrNull)
        assertEquals(subscription["status"]?.jsonPrimitive?.content, SubscriptionStatus.ACTIVE.name)
    }

    @Test
    fun `cancelling a subscription`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        client.post("/api/v1/wallet") {
            header(HttpHeaders.Authorization, authHeader)
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        val method = addPaymentMethodAndGetIt(client, authHeader)

        val request = SubscribeToAuthorRequest(
            authorId = 200L,
            amount = 50.0,
            method["paymentMethodId"]?.jsonPrimitive?.content
                ?: throw NullPointerException("paymentId is null")
        )

        val response = client.post("/api/v1/wallet/subscription") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        SubscribeToAuthorRequest.serializer(),
                        request
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }
        val createdRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for subscribe: $createdRaw")
        val subscription = runCatching {
            testJson.parseToJsonElement(createdRaw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse subscription JSON: $createdRaw", it) }

        assertNotNull(subscription, "Failed to get subscription for cancel test")

        val responseDelete = client.delete("/api/v1/wallet/subscription") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("subscriptionId", subscription["subscriptionId"]?.jsonPrimitive?.content)
        }
        assertEquals(HttpStatusCode.OK, responseDelete.status)
    }

    @Test
    fun `supporting an author sends transfer message and returns support response`() = testApplication {
        application { module(testServiceConfig()) }
        val authHeader = "Bearer $jwtToken"
        client.post("/api/v1/wallet") {
            header(HttpHeaders.Authorization, authHeader)
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        val paymentMethod = addPaymentMethodAndGetIt(client, authHeader)

        val request = SupportAuthorRequest(
            authorId = 200L,
            amount = 0.0,
            paymentMethod["paymentMethodId"]?.jsonPrimitive?.content
                ?: throw NullPointerException("paymentId is null")
        )

        val response = client.post("/api/v1/wallet/support") {
            header(HttpHeaders.Authorization, authHeader)
            setBody(
                TextContent(
                    text = testJson.encodeToString(
                        SupportAuthorRequest.serializer(),
                        request
                    ),
                    contentType = ContentType.Application.Json
                )
            )
        }
        val createdRaw = response.bodyAsText()
        assertEquals(HttpStatusCode.OK, response.status, "Unexpected status for support: $createdRaw")
        val support = runCatching {
            testJson.parseToJsonElement(createdRaw).jsonObject
        }.getOrElse { throw AssertionError("Failed to parse support JSON: $createdRaw", it) }
        assertEquals(100L, support["userId"]?.jsonPrimitive?.longOrNull)
        assertEquals(200L, support["authorId"]?.jsonPrimitive?.longOrNull)
        assertEquals(0.0, support["amount"]?.jsonPrimitive?.doubleOrNull)
    }

}