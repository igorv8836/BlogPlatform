package com.example.data.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object CommentEditsTable : Table("comment_edits") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val commentId = reference("comment_id", CommentsTable)
    val editorId = text("editor_id")
    val oldBody = text("old_body")
    val newBody = text("new_body")
    val editedAt = timestampWithTimeZone("edited_at")
}