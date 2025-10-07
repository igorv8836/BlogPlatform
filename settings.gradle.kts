rootProject.name = "BlogPlatform"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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
//findProject(":example_service")?.name = "example_service"
include(":core:client")
//findProject(":core:client")?.name = "client"
include(":core:common")
//findProject(":core:common")?.name = "common"
