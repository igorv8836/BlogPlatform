package com.example.data.repositories

import com.example.data.db.tables.*
import com.example.utils.TimeUtils
import io.ktor.server.plugins.*
import kotlinx.datetime.toDeprecatedInstant
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import support.AuthorRole
import support.MessageType
import support.TicketStatus
import support.request.*
import support.response.TicketMessageResponse
import support.response.TicketResponse
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

class TicketRepository {
    fun create(authorId: String, req: CreateTicketRequest): TicketResponse {
        val id = transaction {
            val id = UUID.randomUUID()
            TicketsTable.insert {
                it[TicketsTable.id] = id
                it[TicketsTable.authorId] = authorId
                it[subject] = req.subject
                it[body] = req.body
                it[status] = TicketStatus.OPEN
                it[assigneeId] = null
                it[createdAt] = now()
                it[updatedAt] = now()
                it[closedAt] = null
            }
            logStatus(id, null, TicketStatus.OPEN, authorId)
            id
        }
        return transaction {
            findById(id) ?: throw NotFoundException()
        }
    }

    fun assignSystem(req: AssignRequest, actorId: String): TicketResponse = transaction {
        val id = UUID.fromString(req.ticketId)
        val currStatus = currentStatus(id)
        TicketsTable.update({ TicketsTable.id eq id }) {
            it[assigneeId] = req.assigneeId
            it[status] = TicketStatus.IN_PROGRESS
            it[updatedAt] = now()
        }
        logStatus(id, currStatus, TicketStatus.IN_PROGRESS, actorId)
        addMessage(id, req.assigneeId, AuthorRole.SYSTEM, MessageType.MESSAGE, "Назначен исполнитель: ${req.assigneeId}")
        findById(id) ?: throw NotFoundException()
    }

    fun requestClarification(req: ClarifyRequest, moderatorId: String): TicketResponse = transaction {
        val id = UUID.fromString(req.ticketId)
        TicketsTable.update({ TicketsTable.id eq id }) {
            it[status] = TicketStatus.WAITING_USER
            it[updatedAt] = now()
        }
        logStatus(id, currentStatus(id), TicketStatus.WAITING_USER, moderatorId)
        addMessage(id, moderatorId, AuthorRole.MODERATOR, MessageType.CLARIFICATION_REQUEST, req.message)
        findById(id) ?: throw NotFoundException()
    }

    fun applyModeration(req: ModerateRequest, moderatorId: String): TicketResponse = transaction {
        val id = UUID.fromString(req.ticketId)
        ModerationActionsTable.insert {
            it[ticketId] = id
            it[ModerationActionsTable.moderatorId] = moderatorId
            it[targetType] = req.targetType
            it[targetId] = req.targetId
            it[action] = req.action
            it[reason] = req.reason
            it[createdAt] = now()
        }
        addMessage(id, moderatorId, AuthorRole.MODERATOR, MessageType.MODERATION_NOTE, req.note ?: "Модерация применена")
        findById(id) ?: throw NotFoundException()
    }

    fun answer(req: AnswerRequest, moderatorId: String): TicketResponse = transaction {
        val id = UUID.fromString(req.ticketId)
        addMessage(id, moderatorId, AuthorRole.MODERATOR, MessageType.ANSWER, req.message)
        TicketsTable.update({ TicketsTable.id eq id }) {
            it[status] = TicketStatus.ANSWERED
            it[updatedAt] = now()
        }
        logStatus(id, currentStatus(id), TicketStatus.ANSWERED, moderatorId)
        findById(id) ?: throw NotFoundException()
    }

    fun close(req: CloseRequest, actorId: String): TicketResponse = transaction {
        val id = UUID.fromString(req.ticketId)
        TicketsTable.update({ TicketsTable.id eq id }) {
            it[status] = TicketStatus.CLOSED
            it[updatedAt] = now()
            it[closedAt] = now()
        }
        logStatus(id, currentStatus(id), TicketStatus.CLOSED, actorId)
        findById(id) ?: throw NotFoundException()
    }

    fun rate(req: RateRequest, userId: String) = transaction {
        val id = UUID.fromString(req.ticketId)
        TicketRatingsTable.insert {
            it[ticketId] = id
            it[raterId] = userId
            it[score] = req.score
            it[comment] = req.comment
            it[createdAt] = now()
        }
    }

    fun list(filter: TicketListRequest): List<TicketResponse> = transaction {
        var query = TicketsTable.selectAll()
        filter.status?.let { s -> query = query.where(TicketsTable.status eq s) }
        filter.assigneeId?.let { a -> query = query.where((TicketsTable.assigneeId eq a) and (TicketsTable.status neq TicketStatus.CLOSED)) }
        query.orderBy(TicketsTable.createdAt, SortOrder.DESC)
            .limit(filter.limit ?: 20)
            .offset((filter.offset ?: 0).toLong())
            .map { toTicket(it) }
    }

    fun messages(ticketId: UUID): List<TicketMessageResponse> = transaction {
        TicketMessagesTable
            .selectAll()
            .where(TicketMessagesTable.ticketId eq ticketId)
            .orderBy(TicketMessagesTable.createdAt, SortOrder.ASC)
            .map { toMessage(it) }
    }

    private fun addMessage(
        ticketId: UUID,
        authorId: String,
        role: AuthorRole,
        type: MessageType,
        body: String
    ) {
        TicketMessagesTable.insert {
            it[TicketMessagesTable.ticketId] = ticketId
            it[TicketMessagesTable.authorId] = authorId
            it[authorRole] = role
            it[TicketMessagesTable.type] = type
            it[TicketMessagesTable.body] = body
            it[createdAt] = now()
        }
    }

    private fun currentStatus(id: UUID): TicketStatus? =
        TicketsTable.selectAll().where(TicketsTable.id eq id).firstOrNull()?.get(TicketsTable.status)

    private fun logStatus(id: UUID, from: TicketStatus?, to: TicketStatus, actorId: String) {
        TicketStatusLogsTable.insert {
            it[ticketId] = id
            it[fromStatus] = from
            it[toStatus] = to
            it[TicketStatusLogsTable.actorId] = actorId
            it[createdAt] = now()
        }
    }

    private fun findById(id: UUID): TicketResponse? =
        TicketsTable.selectAll().where(TicketsTable.id eq id).firstOrNull()?.let { toTicket(it) }

    @OptIn(ExperimentalTime::class)
    private fun toTicket(row: ResultRow) = TicketResponse(
        id = row[TicketsTable.id].toString(),
        authorId = row[TicketsTable.authorId],
        subject = row[TicketsTable.subject],
        body = row[TicketsTable.body],
        status = row[TicketsTable.status],
        assigneeId = row[TicketsTable.assigneeId],
        createdAt = row[TicketsTable.createdAt].toInstant().toKotlinInstant().toDeprecatedInstant(),
        updatedAt = row[TicketsTable.updatedAt].toInstant().toKotlinInstant().toDeprecatedInstant(),
        closedAt = row[TicketsTable.closedAt]?.toInstant()?.toKotlinInstant()?.toDeprecatedInstant()
    )

    @OptIn(ExperimentalTime::class)
    private fun toMessage(row: ResultRow) = TicketMessageResponse(
        id = row[TicketMessagesTable.id],
        ticketId = row[TicketMessagesTable.ticketId].toString(),
        authorId = row[TicketMessagesTable.authorId],
        authorRole = row[TicketMessagesTable.authorRole],
        type = row[TicketMessagesTable.type],
        body = row[TicketMessagesTable.body],
        createdAt = row[TicketMessagesTable.createdAt].toInstant().toKotlinInstant().toDeprecatedInstant()
    )

    private fun now() = TimeUtils.currentUtcOffsetDateTime()
}
