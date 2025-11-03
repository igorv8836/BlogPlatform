package support.response

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import support.AuthorRole
import support.MessageType
import support.TicketStatus

@Serializable
data class TicketResponse(
    val id: String,
    val authorId: String,
    val subject: String,
    val body: String,
    val status: TicketStatus,
    val assigneeId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val closedAt: Instant?
)

@Serializable
data class TicketMessageResponse(
    val id: Long,
    val ticketId: String,
    val authorId: String,
    val authorRole: AuthorRole,
    val type: MessageType,
    val body: String,
    val createdAt: Instant
)