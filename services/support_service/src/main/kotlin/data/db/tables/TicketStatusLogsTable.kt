package com.example.data.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import support.TicketStatus

object TicketStatusLogsTable : Table("ticket_status_logs") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val ticketId = reference("ticket_id", TicketsTable.id)
    val fromStatus = enumerationByName("from_status", 32, TicketStatus::class).nullable()
    val toStatus = enumerationByName("to_status", 32, TicketStatus::class)
    val actorId = text("actor_id")
    val createdAt = timestampWithTimeZone("created_at")
}