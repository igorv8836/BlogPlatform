rootProject.name = "BlogPlatform"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

include(":example_service")
findProject(":example_service")?.name = "example_service"
include(":core")
findProject(":core")?.name = "core"
include(":client")
findProject(":client")?.name = "client"
