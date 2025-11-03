package com.example.data.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import support.TicketStatus

object TicketsTable : Table("tickets") {
    val id = uuid("id")
    override val primaryKey = PrimaryKey(id)
    val authorId = text("author_id")
    val subject = text("subject")
    val body = text("body")
    val status = enumerationByName("status", 32, TicketStatus::class)
    val assigneeId = text("assignee_id").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    val closedAt = timestampWithTimeZone("closed_at").nullable()
}