package com.example.data.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone


object TicketRatingsTable : Table("ticket_ratings") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val ticketId = reference("ticket_id", TicketsTable.id)
    val raterId = text("rater_id")
    val score = integer("score")
    val comment = text("comment").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}