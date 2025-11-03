package com.example.data.repositories

import com.example.constants.ErrorType
import com.example.constants.ForbiddenException
import com.example.data.db.tables.CommentEditsTable
import com.example.data.db.tables.CommentsTable
import com.example.data.db.tables.MentionsTable
import com.example.data.db.tables.ModerationLogsTable
import com.example.utils.TimeUtils
import comments.request.CreateCommentRequest
import comments.request.EditCommentRequest
import comments.request.TargetType
import comments.response.CommentResponse
import io.ktor.server.plugins.*
import kotlinx.datetime.toDeprecatedInstant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

class CommentRepository {
    fun create(authorId: String, req: CreateCommentRequest): CommentResponse? {
        val id = transaction {
            val mentions = sanitizeMentions(req.mentions)
            val id = CommentsTable.insertAndGetId {
                it[targetType] = req.targetType.value
                it[targetId] = req.targetId
                it[parentId] = req.parentId?.let(::uuidEntityId)
                it[CommentsTable.authorId] = authorId
                it[body] = req.body
                it[createdAt] = TimeUtils.currentUtcOffsetDateTime()
                it[updatedAt] = TimeUtils.currentUtcOffsetDateTime()
                it[edited] = false
                it[isDeleted] = false
                it[isHidden] = false
            }.value
            replaceMentions(id, mentions)
            id
        }
        return transaction {
            findById(id)
        }
    }

    fun reply(authorId: String, parent: UUID, req: EditCommentRequest): CommentResponse? = transaction {
        val parentRow = getCommentRow(parent)
        val mentions = sanitizeMentions(req.mentions)
        val id = CommentsTable.insertAndGetId {
            it[targetType] = parentRow[CommentsTable.targetType]
            it[targetId] = parentRow[CommentsTable.targetId]
            it[parentId] = parentRow[CommentsTable.id]
            it[CommentsTable.authorId] = authorId
            it[CommentsTable.body] = req.body
            it[createdAt] = TimeUtils.currentUtcOffsetDateTime()
            it[updatedAt] = TimeUtils.currentUtcOffsetDateTime()
            it[edited] = false
            it[isDeleted] = false
            it[isHidden] = false
        }.value
        replaceMentions(id, mentions)
        findById(id)
    }

    fun edit(commentId: UUID, editorId: String, req: EditCommentRequest): CommentResponse? = transaction {
        val row = getCommentRowForAuthor(commentId, editorId)
        CommentEditsTable.insert {
            it[CommentEditsTable.commentId] = commentId
            it[CommentEditsTable.editorId] = editorId
            it[oldBody] = row[CommentsTable.body]
            it[CommentEditsTable.newBody] = req.body
            it[editedAt] = TimeUtils.currentUtcOffsetDateTime()
        }
        CommentsTable.update({ CommentsTable.id eq commentId }) {
            it[body] = req.body
            it[updatedAt] = TimeUtils.currentUtcOffsetDateTime()
            it[edited] = true
        }
        req.mentions?.let { replaceMentions(commentId, sanitizeMentions(it)) }
        findById(commentId)
    }

    fun softDelete(commentId: UUID, actorId: String) = transaction {
        getCommentRowForAuthor(commentId, actorId)
        CommentsTable.update({ CommentsTable.id eq commentId }) { it[isDeleted] = true }
        ModerationLogsTable.insert {
            it[this.commentId] = commentId
            it[action] = "delete"
            it[ModerationLogsTable.actorId] = actorId
            it[createdAt] = TimeUtils.currentUtcOffsetDateTime()
        }
    }

    fun hide(commentId: UUID, moderatorId: String, reason: String?) = transaction {
        getCommentRow(commentId)
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
        getCommentRow(commentId)
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
        getCommentRowForAuthor(commentId, authorId)
        CommentsTable.update({ CommentsTable.id eq commentId }) {
            it[pinnedByAuthorAt] = TimeUtils.currentUtcOffsetDateTime()
        }
    }

    fun unpin(commentId: UUID, authorId: String) = transaction {
        getCommentRowForAuthor(commentId, authorId)
        CommentsTable.update({ CommentsTable.id eq commentId }) {
            it[pinnedByAuthorAt] = null
        }
    }

    fun list(targetType: TargetType, targetId: String, limit: Int, offset: Int): List<CommentResponse> = transaction {
        CommentsTable
            .selectAll()
            .where((CommentsTable.targetType eq targetType.value) and (CommentsTable.targetId eq targetId))
            .orderBy(CommentsTable.createdAt, SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .map { toResponse(it) }
    }

    private fun findById(id: UUID): CommentResponse? {
        return CommentsTable
            .selectAll()
            .where(CommentsTable.id eq id)
            .firstOrNull()
            ?.let { toResponse(it) }
    }

    private fun getCommentRow(commentId: UUID): ResultRow =
        CommentsTable.selectAll().where(CommentsTable.id eq commentId).firstOrNull()
            ?: throw BadRequestException("Comment not found")

    private fun getCommentRowForAuthor(commentId: UUID, userId: String): ResultRow {
        val row = getCommentRow(commentId)
        if (row[CommentsTable.authorId] != userId) {
            throw ForbiddenException(ErrorType.FORBIDDEN.message)
        }
        return row
    }

    private fun replaceMentions(commentId: UUID, mentions: List<String>) {
        MentionsTable.deleteWhere { MentionsTable.commentId eq commentId }
        mentions.forEach { userId ->
            MentionsTable.insert {
                it[this.commentId] = commentId
                it[this.mentionedUserId] = userId
                it[this.createdAt] = TimeUtils.currentUtcOffsetDateTime()
            }
        }
    }

    private fun sanitizeMentions(mentions: List<String>?): List<String> =
        mentions
            .orEmpty()
            .map(String::trim)
            .filter { it.isNotEmpty() }
            .distinct()

    private fun loadMentions(commentId: UUID): List<String> =
        MentionsTable
            .select(MentionsTable.commentId eq commentId)
            .orderBy(MentionsTable.createdAt, SortOrder.ASC)
            .map { it[MentionsTable.mentionedUserId] }

    @OptIn(ExperimentalTime::class)
    private fun toResponse(row: ResultRow): CommentResponse {
        val commentId = row[CommentsTable.id].value
        return CommentResponse(
            id = commentId.toString(),
            targetType = row[CommentsTable.targetType],
            targetId = row[CommentsTable.targetId],
            parentId = row[CommentsTable.parentId]?.value?.toString(),
            authorId = row[CommentsTable.authorId],
            body = row[CommentsTable.body],
            createdAt = row[CommentsTable.createdAt].toInstant().toKotlinInstant().toDeprecatedInstant(),
            updatedAt = row[CommentsTable.updatedAt].toInstant().toKotlinInstant().toDeprecatedInstant(),
            edited = row[CommentsTable.edited],
            isDeleted = row[CommentsTable.isDeleted],
            isHidden = row[CommentsTable.isHidden],
            pinnedByAuthorAt = row[CommentsTable.pinnedByAuthorAt]?.toInstant()?.toKotlinInstant()?.toDeprecatedInstant(),
            mentions = loadMentions(commentId),
        )
    }

    private fun uuidEntityId(rawId: String): EntityID<UUID> =
        runCatching { UUID.fromString(rawId) }
            .map { EntityID(it, CommentsTable) }
            .getOrElse { throw BadRequestException("Invalid parent comment id") }
}
