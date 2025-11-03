package support.request

import kotlinx.serialization.Serializable
import support.ModAction
import support.TicketStatus

@Serializable
data class CreateTicketRequest(
    val subject: String,
    val body: String
)

@Serializable
data class AssignRequest(
    val ticketId: String,
    val assigneeId: String
)

@Serializable
data class ClarifyRequest(
    val ticketId: String,
    val message: String
)

@Serializable
data class AnswerRequest(
    val ticketId: String,
    val message: String
)

@Serializable
data class CloseRequest(
    val ticketId: String
)

@Serializable
data class RateRequest(
    val ticketId: String,
    val score: Int,
    val comment: String? = null
)

@Serializable
data class ModerateRequest(
    val ticketId: String,
    val targetType: String,
    val targetId: String,
    val action: ModAction,
    val reason: String? = null,
    val note: String? = null
)

@Serializable
data class TicketListRequest(
    val status: TicketStatus? = null,
    val assigneeId: String? = null,
    val limit: Int? = 20,
    val offset: Int? = 0
)