package com.example.data.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import support.AuthorRole
import support.MessageType

object TicketMessagesTable : Table("ticket_messages") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val ticketId = reference("ticket_id", TicketsTable.id)
    val authorId = text("author_id")
    val authorRole = enumerationByName("author_role", 16, AuthorRole::class)
    val type = enumerationByName("type", 32, MessageType::class)
    val body = text("body")
    val createdAt = timestampWithTimeZone("created_at")
}