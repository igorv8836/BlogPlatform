package com.example.data.repositories

import com.example.data.db.tables.ReactionsTable
import com.example.utils.TimeUtils
import comments.ReactionType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

class ReactionRepository {
    fun set(commentId: UUID, userId: String, type: ReactionType) = transaction {
        ReactionsTable.insertIgnore {
            it[this.commentId] = commentId
            it[this.userId] = userId
            it[this.type] = type
            it[this.createdAt] = TimeUtils.currentUtcOffsetDateTime()
        }
    }

    fun remove(commentId: UUID, userId: String, type: ReactionType) = transaction {
        ReactionsTable.deleteWhere {
            (ReactionsTable.commentId eq commentId) and (ReactionsTable.userId eq userId) and (ReactionsTable.type eq type)
        }
    }
}