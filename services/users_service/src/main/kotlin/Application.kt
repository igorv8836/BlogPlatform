
import com.example.commonPlugins.DatabaseFactory
import com.example.commonPlugins.configureCommonRouting
import com.example.commonPlugins.configureKoin
import com.example.commonPlugins.configureMonitoring
import com.example.commonPlugins.configureOpenApi
import com.example.commonPlugins.configureSerialization
import com.example.config.ConfigName
import com.example.config.ServiceConfig
import com.example.config.getServiceConfig
import data.dataModule
import data.db.tables.BanTable
import data.db.tables.FollowTable
import data.db.tables.ReportTable
import data.db.tables.UserTable
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import routes.SupportRoutes
import routes.userRouting
import security.configureSecurity

fun main(args: Array<String>) {
    val config = getServiceConfig(ConfigName.USER_SERVICE)
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

    configureSecurity(SupportRoutes.tokenConfig)
    configureCommonRouting()
    DatabaseFactory.initializationDatabase(
        config = config,
        tables = arrayOf(
            UserTable,
            FollowTable,
            BanTable,
            ReportTable
        )
    )

    routing {
        userRouting()
    }
}
