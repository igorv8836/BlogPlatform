plugins {
    `kotlin-dsl`
}

repositories{
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies{
    compileOnly(libs.plugins.kotlin.serialization.toDep())
}

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}