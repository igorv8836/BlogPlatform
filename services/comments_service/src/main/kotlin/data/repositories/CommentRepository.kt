package com.example.data.repositories

import com.example.data.db.tables.CommentEditsTable
import com.example.data.db.tables.CommentsTable
import com.example.data.db.tables.ModerationLogsTable
import com.example.utils.TimeUtils
import comments.request.CreateCommentRequest
import comments.response.CommentResponse
import kotlinx.datetime.toDeprecatedInstant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

class CommentRepository {
    fun create(authorId: String, req: CreateCommentRequest): CommentResponse? = transaction {
        val id = CommentsTable.insertAndGetId {
            it[targetType] = req.targetType
            it[targetId] = req.targetId
            it[parentId] = req.parentId?.let(UUID::fromString)
            it[CommentsTable.authorId] = authorId
            it[body] = req.body
            it[createdAt] = TimeUtils.currentUtcOffsetDateTime()
            it[updatedAt] = TimeUtils.currentUtcOffsetDateTime()
            it[edited] = false
            it[isDeleted] = false
            it[isHidden] = false
        }.value
        findById(id)
    }

    fun reply(authorId: String, parent: UUID, body: String): CommentResponse? = transaction {
        val parentRow = CommentsTable.select(CommentsTable.id eq EntityID(parent, CommentsTable)).single()
        val id = CommentsTable.insertAndGetId {
            it[targetType] = parentRow[CommentsTable.targetType]
            it[targetId] = parentRow[CommentsTable.targetId]
            it[parentId] = parent
            it[CommentsTable.authorId] = authorId
            it[CommentsTable.body] = body
            it[createdAt] = TimeUtils.currentUtcOffsetDateTime()
            it[updatedAt] = TimeUtils.currentUtcOffsetDateTime()
            it[edited] = false
            it[isDeleted] = false
            it[isHidden] = false
        }.value
        findById(id)
    }

    fun edit(commentId: UUID, editorId: String, newBody: String): CommentResponse? = transaction {
        val row = CommentsTable.select(CommentsTable.id eq commentId).single()
        CommentEditsTable.insert {
            it[CommentEditsTable.commentId] = commentId
            it[CommentEditsTable.editorId] = editorId
            it[oldBody] = row[CommentsTable.body]
            it[CommentEditsTable.newBody] = newBody
            it[editedAt] = TimeUtils.currentUtcOffsetDateTime()
        }
        CommentsTable.update({ CommentsTable.id eq commentId }) {
            it[body] = newBody
            it[updatedAt] = TimeUtils.currentUtcOffsetDateTime()
            it[edited] = true
        }
        findById(commentId)
    }

    fun softDelete(commentId: UUID, actorId: String) = transaction {
        CommentsTable.update({ CommentsTable.id eq commentId }) { it[isDeleted] = true }
        ModerationLogsTable.insert {
            it[this.commentId] = commentId
            it[action] = "delete"
            it[ModerationLogsTable.actorId] = actorId
            it[createdAt] = TimeUtils.currentUtcOffsetDateTime()
        }
    }

    fun hide(commentId: UUID, moderatorId: String, reason: String?) = transaction {
        CommentsTable.update({ CommentsTable.id eq commentId }) {
            it[isHidden] = true
            it[hiddenBy] = moderatorId
            it[hiddenReason] = reason
        }
        ModerationLogsTable.insert {
            it[this.commentId] = commentId
            it[action] = "hide"
            it[actorId] = moderatorId
            it[details] = reason
            it[createdAt] = TimeUtils.currentUtcOffsetDateTime()
        }
    }

    fun restore(commentId: UUID, moderatorId: String) = transaction {
        CommentsTable.update({ CommentsTable.id eq commentId }) {
            it[isHidden] = false
            it[hiddenBy] = null
            it[hiddenReason] = null
        }
        ModerationLogsTable.insert {
            it[this.commentId] = commentId
            it[action] = "restore"
            it[actorId] = moderatorId
            it[createdAt] = TimeUtils.currentUtcOffsetDateTime()
        }
    }

    fun pin(commentId: UUID, authorId: String) = transaction {
        CommentsTable.update({ CommentsTable.id eq commentId and (CommentsTable.authorId eq authorId) }) {
            it[pinnedByAuthorAt] = TimeUtils.currentUtcOffsetDateTime()
        }
    }

    fun unpin(commentId: UUID, authorId: String) = transaction {
        CommentsTable.update({ CommentsTable.id eq commentId and (CommentsTable.authorId eq authorId) }) {
            it[pinnedByAuthorAt] = null
        }
    }

    fun list(targetType: String, targetId: String, limit: Int, offset: Int): List<CommentResponse> = transaction {
        CommentsTable
            .select((CommentsTable.targetType eq targetType) and (CommentsTable.targetId eq targetId))
            .orderBy(CommentsTable.createdAt, SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .map { toResponse(it) }
    }

    private fun findById(id: UUID): CommentResponse? {
        return CommentsTable.select(CommentsTable.id eq id).firstOrNull()?.let { toResponse(it) }
    }

    @OptIn(ExperimentalTime::class)
    private fun toResponse(row: ResultRow) = CommentResponse(
        id = row[CommentsTable.id].value.toString(),
        targetType = row[CommentsTable.targetType],
        targetId = row[CommentsTable.targetId],
        parentId = row[CommentsTable.parentId]?.value.toString().takeIf { it != "null" },
        authorId = row[CommentsTable.authorId],
        body = row[CommentsTable.body],
        createdAt = row[CommentsTable.createdAt].toInstant().toKotlinInstant().toDeprecatedInstant(),
        updatedAt = row[CommentsTable.updatedAt].toInstant().toKotlinInstant().toDeprecatedInstant(),
        edited = row[CommentsTable.edited],
        isDeleted = row[CommentsTable.isDeleted],
        isHidden = row[CommentsTable.isHidden],
        pinnedByAuthorAt = row[CommentsTable.pinnedByAuthorAt]?.toInstant()?.toKotlinInstant()?.toDeprecatedInstant()
    )
}