package p1

import com.rabbitmq.client.*

fun main() {
    val factory = ConnectionFactory().apply {
        host = "localhost"
        port = 5672
    }

    val connection = factory.newConnection()
    val channel = connection.createChannel()

    try {
        val student = "ikbo-06_nilov"

        // Для durable и auto-delete очередей:
        /*channel.queueDeclare(
            student,
            true,
            false,
            false,
            null
        )*/
        // Для эксклюзивной очереди:
        channel.queueDeclare(
            student,
            false,
            true,
            false,
            null
        )

        println(" [*] Waiting for messages. To exit press CTRL+C")

        val deliverCallback = { consumerTag: String, delivery: Delivery ->
            val message = String(delivery.body, charset("UTF-8"))
            println(" [x] Received '$message' from $consumerTag")
            // Имитация обработки
            Thread.sleep(1000)
            println(" [x] Done processing '$message'")
        }

        val cancelCallback = { consumerTag: String ->
            println(" [x] Consumer $consumerTag was cancelled")
        }

        channel.basicConsume(
            student,
            true,
            deliverCallback,
            cancelCallback
        )

        println("Consumer is running... Press Enter to stop.")
        readlnOrNull()

    } catch (e: Exception) {
        println("Error occurred: ${e.message}")
        e.printStackTrace()
    } finally {
        channel.close()
        connection.close()
    }
}