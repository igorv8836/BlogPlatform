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
include(":shared:client")
include(":shared:common")
include(":shared:contracts")

include(":services:comments_service")
include(":services:support_service")

include("pract5")

include(":services:users_service")
include("services:wallet_service")
include("services:payment_service")
include("services:posts_service")
