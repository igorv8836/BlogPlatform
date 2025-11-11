package org.example

import com.example.commonPlugins.*
import com.example.config.ConfigName
import com.example.config.ServiceConfig
import com.example.config.getServiceConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.example.data.dataModule
import org.example.data.db.tables.ComplaintsTable
import org.example.data.db.tables.HiddenAuthorsTable
import org.example.data.db.tables.PostsTable
import org.example.routes.configurePostsRouting

fun main(args: Array<String>) {
    val config = getServiceConfig(ConfigName.POSTS_SERVICE)
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
            PostsTable,
            ComplaintsTable,
            HiddenAuthorsTable
        )
    )

    routing {
        configurePostsRouting()
    }
}
