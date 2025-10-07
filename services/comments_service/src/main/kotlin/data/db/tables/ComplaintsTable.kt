package com.example.data.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object ComplaintsTable : Table("comment_complaints") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val commentId = reference("comment_id", CommentsTable)
    val complainantId = text("complainant_id")
    val reason = text("reason")
    val status = enumerationByName("status", 32, ComplaintStatus::class)
        .default(ComplaintStatus.NEW)
    val createdAt = timestampWithTimeZone("created_at")
    val processedAt = timestampWithTimeZone("processed_at").nullable()
    val moderatorId = text("moderator_id").nullable()
}

enum class ComplaintStatus {
    NEW,
    IN_REVIEW,
    REJECTED,
    APPROVED
}