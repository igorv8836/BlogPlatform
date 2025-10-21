package p1

import com.rabbitmq.client.ConnectionFactory

fun main() {
    val factory = ConnectionFactory().apply {
        host = "localhost"
        port = 5672
    }

    val connection = factory.newConnection()
    val channel = connection.createChannel()

    try {
        val student = "ikbo-06_nilov"

        // 1. Эксклюзивная очередь
        /*channel.queueDeclare(
            student,
            false,
            true,
            false,
            null
        )*/

        // 2. Сохраняемая очередь
        /*channel.queueDeclare(
            student,
            true,
            false,
            false,
            null
        )*/

        // 3. Автоудаляемая очередь
        channel.queueDeclare(
            student,
            false,
            false,
            true,
            null
        )

        val message = "$student first message from Kotlin!"
        println(" [x] Sending '$message'")

        channel.basicPublish(
            "",
            student,
            null,
            message.toByteArray(charset("UTF-8"))
        )

        println(" [x] Sent '$message'")
    } catch (e: Exception) {
        println("Error occurred: ${e.message}")
        e.printStackTrace()
    } finally {
        channel.close()
        connection.close()
    }
}