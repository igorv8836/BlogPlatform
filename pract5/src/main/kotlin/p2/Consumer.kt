package p2

import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import java.util.*
import java.util.concurrent.CountDownLatch


fun main() {
    val factory = ConnectionFactory().apply {
        host = "localhost"
        port = 5672
    }

    val connection = factory.newConnection()
    val channel = connection.createChannel()

    try {
        var exchangeName = ""
        var queueName = ""
        val type: String? = readlnOrNull()
        val student = "ikbo-06_nilov " + Calendar.getInstance().time


        when (type) {
            "1" -> {
                // Вариант 1: fanout
                exchangeName = "logs_fanout"
                channel.exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT)
                queueName = channel.queueDeclare(
                    student,
                    true,
                    false,
                    false,
                    null
                ).queue
                channel.queueBind(queueName, exchangeName, "")
            }

            "2" -> {
                // Вариант 2: direct
                exchangeName = "logs_direct"
                channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT)
                queueName = channel.queueDeclare(
                    student,
                    false,
                    true,
                    true,
                    null
                ).queue
                val bindingKey = "info"
                channel.queueBind(queueName, exchangeName, bindingKey)
            }

            "3" -> {
                // Вариант 3: topic
                exchangeName = "logs_topic"
                channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC)
                queueName = channel.queueDeclare(
                    student,
                    true,
                    false,
                    false,
                    null
                ).queue
                val bindingKey = "*.info"
                channel.queueBind(queueName, exchangeName, bindingKey)
            }

        }


        println(" [*] Waiting for messages from exchange '$exchangeName'. To exit press CTRL+C")

        channel.basicQos(1)
        val latch = CountDownLatch(1)

        val deliverCallback = { consumerTag: String, delivery: Delivery ->
            val message = String(delivery.body, charset("UTF-8"))
            try {
                println(" [x] Received '$message' from $consumerTag")
                val sleepSymbol = when (exchangeName) {
                    "logs_fanout" -> '#'
                    "logs_topic" -> '*'
                    else -> '-'
                }
                val sleepTime = message.count { it == sleepSymbol } * 1000L
                Thread.sleep(sleepTime)
                println(" [x] Done processing '$message' (slept for ${sleepTime / 1000}s)")
            } catch (_: InterruptedException) {
                println(" [x] Processing interrupted for '$message'")
                Thread.currentThread().interrupt()
            } finally {
                channel.basicAck(delivery.envelope.deliveryTag, false)
                println(" [x] Acknowledged message '$message'")
            }
        }

        val cancelCallback = { consumerTag: String ->
            println(" [x] Consumer $consumerTag was cancelled")
        }

        channel.basicConsume(queueName, false, deliverCallback, cancelCallback)

        Runtime.getRuntime().addShutdownHook(Thread {
            println("Shutting down consumer...")
            try {
                channel.close()
                connection.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            latch.countDown()
        })

        latch.await()

    } catch (e: Exception) {
        println("Error occurred: ${e.message}")
        e.printStackTrace()
    } finally {
        try {
            channel.close()
            connection.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}