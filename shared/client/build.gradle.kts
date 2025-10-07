plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

dependencies {
    api(projects.shared.common)

    api(libs.bundles.ktor.client)

    api(libs.koin.ktor)
    api(libs.koin.core)
}