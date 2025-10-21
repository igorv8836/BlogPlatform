plugins {
    kotlin("jvm")
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.rabbitmq:amqp-client:5.20.0")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Exec>("startRabbitMQContainer") {
    group = "pract5_rabbitMQ"
    description = "Start RabbitMQ container using Docker"

    commandLine("docker", "run", "-d", "--rm", "--name", "rabbitmq", "-p", "5672:5672", "-p", "15672:15672", "rabbitmq:3-management")

    doFirst {
        val checkProcess = ProcessBuilder("docker", "ps", "--filter", "name=rabbitmq", "--format", "{{.Names}}").start()
        val output = checkProcess.inputStream.bufferedReader().readText().trim()
        if (output == "rabbitmq") {
            println("RabbitMQ container 'rabbitmq' is already running.")
        } else {
            println("Starting RabbitMQ container...")
        }
    }
}

tasks.register<Exec>("stopRabbitMQContainer") {
    group = "pract5_rabbitMQ"
    description = "Stop and remove RabbitMQ container"

    commandLine("docker", "stop", "rabbitmq")

    isIgnoreExitValue = true
}

kotlin {
    jvmToolchain(21)
}