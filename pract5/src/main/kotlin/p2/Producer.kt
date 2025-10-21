package p2

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.rabbitmq.client.BuiltinExchangeType

fun main() {
    val factory = ConnectionFactory().apply {
        host = "localhost"
        port = 5672
    }

    val connection = factory.newConnection()
    val channel = connection.createChannel()

    try {
        val type = readlnOrNull()
        var exchangeName = ""
        var routingKey = ""
        var message = ""

        when (type) {
            "1" -> {
                // Вариант 1: fanout
                exchangeName = "logs_fanout"
                routingKey = ""
                message = "Default message with #"
                channel.exchangeDeclare(
                    exchangeName,
                    BuiltinExchangeType.FANOUT
                )
            }
            "2" -> {
                // Вариант 2: direct
                exchangeName = "logs_direct"
                routingKey = "infof"
                message = "Default message with *"
                channel.exchangeDeclare(
                    exchangeName,
                    BuiltinExchangeType.DIRECT
                )
            }
            "3" -> {
                // Вариант 3: topic
                exchangeName = "logs_topic"
                routingKey = "kotlin.info"
                message = "Default message with -"
                channel.exchangeDeclare(
                    exchangeName,
                    BuiltinExchangeType.TOPIC
                )
            }
        }


        println(" [x] Sending '$message' with routing key '$routingKey'")

        var props: AMQP.BasicProperties? = null

        // Варианты 1 и 3: сообщения должны храниться
        if (exchangeName in arrayOf("logs_fanout", "logs_topic")) {
            props = MessageProperties.BASIC.builder()
                .deliveryMode(2) // PERSISTENT_DELIVERY_MODE
                .build()
        }

        channel.basicPublish(
            exchangeName,
            routingKey,
            props,
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