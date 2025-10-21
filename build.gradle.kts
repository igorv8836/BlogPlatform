plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.conventional.commits)
}

subprojects {
    group = "com.example"
    version = "0.0.1"
}

conventionalCommits {
    scopes = listOf(
        "shared",
        "example_service",
        "contract",

        "comments_service",
        "support_service",
        "pract5"
    )
}

//feat	    новая функциональность (feature)
//fix	    исправление ошибки (bug fix)
//docs	    изменение документации
//style	    правки форматирования, не влияющие на логику (пробелы, отступы, prettier)
//refactor	изменение структуры кода без изменения поведения
//perf	    улучшение производительности
//test	    добавление или изменение тестов
//build 	изменения, влияющие на сборку, зависимости (Gradle, npm)
//ci	    изменения конфигурации CI/CD (GitHub Actions, Jenkins)
//chore 	мелкие задачи, не влияющие на код приложения (обновление README, чистка мусора)
//revert	откат предыдущего коммита
