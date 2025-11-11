package org.example.data.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone


object HiddenAuthorsTable : Table("hidden_authors") {
    val userId = long("user_id")
    val authorId = long("author_id")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(userId, authorId)
}