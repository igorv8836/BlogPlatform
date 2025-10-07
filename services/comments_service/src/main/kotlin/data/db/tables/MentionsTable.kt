package com.example.data.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object MentionsTable : Table("comment_mentions") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val commentId = reference("comment_id", CommentsTable)
    val mentionedUserId = text("mentioned_user_id")
    val createdAt = timestampWithTimeZone("created_at")
}