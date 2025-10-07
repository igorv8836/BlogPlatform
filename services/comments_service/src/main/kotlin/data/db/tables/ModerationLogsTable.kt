package com.example.data.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object ModerationLogsTable : Table("moderation_logs") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val commentId = reference("comment_id", CommentsTable)
    val action = text("action")
    val actorId = text("actor_id")
    val details = text("details").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}