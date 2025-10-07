package com.example.data.db.tables

import comments.ReactionType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object ReactionsTable : Table("comment_reactions") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val commentId = reference("comment_id", CommentsTable)
    val userId = text("user_id")
    val type = enumerationByName("type", 20, ReactionType::class)
    val createdAt = timestampWithTimeZone("created_at")
    init { uniqueIndex(commentId, userId, type) }
}