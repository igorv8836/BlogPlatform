package com.example.data.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import support.ModAction

object ModerationActionsTable : Table("moderation_actions") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val ticketId = reference("ticket_id", TicketsTable.id)
    val moderatorId = text("moderator_id")
    val targetType = text("target_type")
    val targetId = text("target_id")
    val action = enumerationByName("action", 32, ModAction::class)
    val reason = text("reason").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}