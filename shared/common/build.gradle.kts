plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

dependencies {
    implementation(libs.bundles.server.ktor.core)
    api(libs.bundles.server.database)

    api(libs.micrometer.prometheus)
    api(libs.ktor.server.metrics.micrometer)
    api(libs.ktor.server.logging)
    api(libs.ktor.server.openapi)
    api(libs.ktor.server.swagger)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.server.auto.head.response)
    api(libs.ktor.server.content.negotiation)
    api(libs.rabbitmq)
}