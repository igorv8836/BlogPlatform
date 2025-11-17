package com.example.data.repositories

import com.example.data.db.tables.NotificationTable
import notification.NotificationType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.*

private fun createTestDatabase(): Database {
    val db = Database.connect(
        url = "jdbc:h2:mem:notification_${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )
    transaction(db) {
        SchemaUtils.create(
            NotificationTable
        )
    }
    return db
}

class NotificationRepositoryTest {

    private lateinit var database: Database
    private lateinit var notificationRepository: NotificationRepository

    @BeforeTest
    fun setUp() {
        database = createTestDatabase()
        notificationRepository = NotificationRepositoryImpl()
    }

    @AfterTest
    fun tearDown() {
        transaction(database) {
            NotificationTable.deleteAll()
        }
    }

    @Test
    fun `create should insert a new notification`() {
        val userId = 1L
        val message = "Hello, this is a test notification"
        val type = NotificationType.BROADCAST

        val insertStatement = notificationRepository.create(message, type, userId)

        val stored = transaction(database) {
            NotificationTable.selectAll().where { NotificationTable.userId eq userId }.singleOrNull()
        }

        assertNotNull(stored)
        assertEquals(message, stored[NotificationTable.message])
        assertEquals(type, stored[NotificationTable.type])
        assertEquals(userId, stored[NotificationTable.userId])
    }

    @Test
    fun `findById should return notifications for given user`() {
        val userId = 2L
        val message1 = "Notification 1"
        val message2 = "Notification 2"
        val type = NotificationType.BROADCAST

        transaction(database) {
            NotificationTable.insert {
                it[this.userId] = userId
                it[this.message] = message1
                it[this.type] = type
                it[this.createdAt] = OffsetDateTime.now()
            }
            NotificationTable.insert {
                it[this.userId] = userId
                it[this.message] = message2
                it[this.type] = type
                it[this.createdAt] = OffsetDateTime.now()
            }
        }

        val response = notificationRepository.findById(userId)

        assertNotNull(response)
        assertEquals(2, response.notifications.size)
        assertTrue(response.notifications.any { it.message == message1 })
        assertTrue(response.notifications.any { it.message == message2 })
    }

    @Test
    fun `findById should return empty list if user has no notifications`() {
        val userId = 3L

        val response = notificationRepository.findById(userId)

        assertNotNull(response)
        assertTrue(response.notifications.isEmpty())
    }

    @Test
    fun `create and then findById should return the created notification`() {
        val userId = 4L
        val message = "Check creation and retrieval"
        val type = NotificationType.BROADCAST

        notificationRepository.create(message, type, userId)
        val response = notificationRepository.findById(userId)

        assertEquals(1, response!!.notifications.size)
        val notification = response.notifications.first()
        assertEquals(message, notification.message)
        assertEquals(type.name, notification.type)
    }
}