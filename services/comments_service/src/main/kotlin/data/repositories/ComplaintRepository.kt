package com.example.data.repositories

import com.example.data.db.tables.ComplaintsTable
import com.example.utils.TimeUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

class ComplaintRepository {
    fun create(commentId: UUID, userId: String, reason: String) = transaction {
        ComplaintsTable.insert {
            it[this.commentId] = commentId
            it[this.complainantId] = userId
            it[this.reason] = reason
            it[this.createdAt] = TimeUtils.currentUtcOffsetDateTime()
        }
    }
}