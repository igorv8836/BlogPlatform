package example

import com.example.config.ServiceConfig
import java.util.*

internal fun testServiceConfig(): ServiceConfig {
    val dbName = "notification_test_${UUID.randomUUID()}"
    return ServiceConfig(
        ktor = ServiceConfig.KtorSection(
            deployment = ServiceConfig.Deployment(
                host = "127.0.0.1",
                port = 0,
            ),
            jwt = ServiceConfig.Jwt(
                issuer = "test-issuer",
                audience = "all",
                jwksUrl = "http://localhost/jwks.json",
                realm = "test-realm",
                secretKey = "secret",
                expirationTime = 172800000
            ),
            rabbitmq = ServiceConfig.RabbitMq(
                uri = "amqp://guest:guest@localhost:5672",
                defaultConnectionName = "test-connection",
                dispatcherThreadPollSize = 1,
                connectionAttempts = 1,
                attemptDelay = 1,
                tls = ServiceConfig.Tls(enabled = false),
                queue = "notification.queue",
                exchange = "notification.exchange",
            )
        ),
        storage = ServiceConfig.Storage(
            driverClassName = "org.h2.Driver",
            jdbcURL = "jdbc:h2:mem:$dbName;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
            user = "sa",
            password = ""
        )
    )
}
