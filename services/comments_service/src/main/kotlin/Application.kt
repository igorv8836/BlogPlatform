package com.example

import com.example.commonPlugins.*
import com.example.config.ConfigName
import com.example.config.ServiceConfig
import com.example.config.getServiceConfig
import com.example.data.dataModule
import com.example.data.db.tables.*
import com.example.routes.commentsRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    val config = getServiceConfig(ConfigName.COMMENTS_SERVICE)
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
            CommentsTable,
            CommentEditsTable,
            ComplaintsTable,
            MentionsTable,
            ModerationLogsTable,
            ReactionsTable,
        )
    )

    routing {
        commentsRouting()
    }
}
