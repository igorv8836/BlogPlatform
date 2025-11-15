package com.example.data.db.tables

import notification.NotificationType
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import java.time.OffsetDateTime

object NotificationTable : UUIDTable("notification") {
    val userId = long("user_id")
    val createdAt = timestampWithTimeZone("created_at").default(OffsetDateTime.now())
    val message = text("message").nullable()
    val type = enumerationByName<NotificationType>("type", 32)
}