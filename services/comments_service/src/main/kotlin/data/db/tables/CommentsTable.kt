package com.example.data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object CommentsTable : UUIDTable("comments") {
    val targetType = text("target_type")
    val targetId = text("target_id")
    val parentId = reference("parent_id", CommentsTable).nullable()
    val authorId = text("author_id")
    val body = text("body")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    val edited = bool("edited")
    val isDeleted = bool("is_deleted")
    val isHidden = bool("is_hidden")
    val pinnedByAuthorAt = timestampWithTimeZone("pinned_by_author_at").nullable()
    val hiddenBy = text("hidden_by").nullable()
    val hiddenReason = text("hidden_reason").nullable()
}