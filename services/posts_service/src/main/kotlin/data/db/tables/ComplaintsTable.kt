package org.example.data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object ComplaintsTable : UUIDTable("post_complaints") {
    val postId = reference("post_id", PostsTable)
    val complainantId = uuid("complainant_id")
    val complainedById = long("complained_by_id")
    val reason = text("reason")
    val status = enumerationByName("status", 32, ComplaintStatus::class)
        .default(ComplaintStatus.NEW)
    val createdAt = timestampWithTimeZone("created_at")
    val processedAt = timestampWithTimeZone("processed_at").nullable()
    val moderatorId = long("moderator_id").nullable()
}

enum class ComplaintStatus {
    NEW,
    IN_REVIEW,
    REJECTED,
    APPROVED
}