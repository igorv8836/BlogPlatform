plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    api(projects.shared.common)
    api(projects.shared.client)
    api(projects.shared.contracts)

    implementation(libs.bundles.server.ktor.core)
    implementation(libs.bundles.server.database)

    implementation(libs.micrometer.prometheus)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.ktor.server.logging)

    implementation(libs.rabbitmq)
}
