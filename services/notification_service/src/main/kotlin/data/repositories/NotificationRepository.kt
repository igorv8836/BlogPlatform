package com.example.data.repositories

import com.example.data.db.tables.NotificationTable
import notification.NotificationType
import notification.response.Notification
import notification.response.NotificationResponse
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.OffsetDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface NotificationRepository {
    fun create(
        message: String,
        type: NotificationType,
        userId: Long,
    ): InsertStatement<Number>
    fun findById(id: Long): NotificationResponse?
}

class NotificationRepositoryImpl(): NotificationRepository {

    override fun create(
        message: String,
        type: NotificationType,
        userId: Long,
    ) = transaction {

        NotificationTable.insert {
            it[this.userId] = userId
            it[this.message] = message
            it[this.type] = type
            it[this.createdAt] = now()
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun findById(id: Long): NotificationResponse = transaction {
        val notifications = NotificationTable
            .selectAll()
            .where { NotificationTable.userId eq id }
            .mapNotNull { row ->
                val message = row[NotificationTable.message] ?: return@mapNotNull null

                Notification(
                    id = Uuid.parse(row[NotificationTable.id].toString()),
                    message = message,
                    createdAt = row[NotificationTable.createdAt].toString(),
                    type = row[NotificationTable.type].name
                )
            }

        NotificationResponse(notifications = notifications)
    }

    private fun now() = OffsetDateTime.now()
}