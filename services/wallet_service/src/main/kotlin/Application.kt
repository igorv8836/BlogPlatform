import com.example.commonPlugins.*
import com.example.config.ConfigName
import com.example.config.ServiceConfig
import com.example.config.getServiceConfig
import data.dataModule
import data.db.tables.PaymentMethodsTable
import data.db.tables.SubscriptionsTable
import data.db.tables.WalletsTable
import data.db.tables.WithdrawalRequestsTable
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import routes.configureWalletRouting

fun main(args: Array<String>) {
    val config = getServiceConfig(ConfigName.WALLET_SERVICE)
    embeddedServer(
        factory = Netty,
        port = config.ktor.deployment.port,
        host = config.ktor.deployment.host,
        module = { module(config) }
    ).start(wait = true)
}

fun Application.module(config: ServiceConfig) {
    configureOpenApi()
    configureMonitoring()
    configureSerialization()
    configureKoin(
        otherModules = listOf(
            dataModule(),
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
            PaymentMethodsTable,
            SubscriptionsTable,
            WalletsTable,
            WithdrawalRequestsTable
        )
    )

    configureWalletRouting()
}
