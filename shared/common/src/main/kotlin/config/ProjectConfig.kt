package com.example.config

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

data class ServiceConfig(
    val ktor: KtorSection,
    val storage: Storage
) {
    data class KtorSection(
        val deployment: Deployment,
        val jwt: Jwt,
        val rabbitmq: RabbitMq
    )

    data class Deployment(
        val host: String,
        val port: Int,
    )

    data class Jwt(
        val issuer: String,
        val audience: String,
        val jwksUrl: String,
        val realm: String,
        val secretKey: String,
        val expirationTime: Long
    )

    data class RabbitMq(
        val uri: String,
        val defaultConnectionName: String,
        val dispatcherThreadPollSize: Int,
        val connectionAttempts: Int,
        val attemptDelay: Int,
        val tls: Tls,

        val queue: String,
        val exchange: String,
    )

    data class Tls(
        val enabled: Boolean
    )

    data class Storage(
        val driverClassName: String,
        val jdbcURL: String,
        val user: String,
        val password: String
    )
}

enum class ConfigName(val value: String) {
    COMMENTS_SERVICE("comments_service"),
    SUPPORT_SERVICE("support_service"),
    USER_SERVICE("user_service"),
    NOTIFICATION_SERVICE("notification_service"),
    POSTS_SERVICE("posts_service"),
    WALLET_SERVICE("wallet_service"),
    PAYMENT_SERVICE("payment_service")
}

fun getServiceConfig(name: ConfigName): ServiceConfig {
    val root = ConfigFactory.load()
    val config = root.getConfig(name.value).extract<ServiceConfig>()
    return config
}
