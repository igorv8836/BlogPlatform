package org.example.data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object PostsTable : UUIDTable("posts") {
    val authorId = long("author_id")
    val title = text("title")
    val content = text("content")
    val tags = array<String>("tags")
    val status = enumeration("status", PostStatus::class)
        .default(PostStatus.PUBLISHED)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at").nullable()
}

enum class PostStatus { BLOCKED, PUBLISHED, MODERATION, DELETED }