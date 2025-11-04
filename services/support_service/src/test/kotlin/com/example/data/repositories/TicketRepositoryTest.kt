package com.example.data.repositories

import com.example.data.db.tables.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import support.AuthorRole
import support.MessageType
import support.ModAction
import support.TicketStatus
import support.request.*
import java.util.*
import kotlin.test.*

class TicketRepositoryTest {

    private lateinit var database: Database
    private lateinit var repository: TicketRepository

    @BeforeTest
    fun setUp() {
        database = Database.connect(
            url = "jdbc:h2:mem:support_${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        transaction(database) {
            SchemaUtils.create(
                TicketsTable,
                TicketMessagesTable,
                TicketRatingsTable,
                TicketStatusLogsTable,
                ModerationActionsTable,
            )
        }
        repository = TicketRepository()
    }

    @Test
    fun `create stores ticket and initial status log`() {
        val response = repository.create(
            authorId = "author-1",
            req = CreateTicketRequest(subject = "Subject", body = "Body text")
        )

        assertEquals("author-1", response.authorId)
        assertEquals(TicketStatus.OPEN, response.status)

        val logs = transaction(database) { TicketStatusLogsTable.selectAll().toList() }
        assertEquals(1, logs.size)
        assertNull(logs.first()[TicketStatusLogsTable.fromStatus])
        assertEquals(TicketStatus.OPEN, logs.first()[TicketStatusLogsTable.toStatus])
    }

    @Test
    fun `assignSystem switches status to in progress adds system message`() {
        val created = repository.create(
            authorId = "author-1",
            req = CreateTicketRequest(subject = "Need help", body = "Fix issue")
        )

        val updated = repository.assignSystem(
            req = AssignRequest(ticketId = created.id, assigneeId = "moderator-1"),
            actorId = "system"
        )

        assertEquals(TicketStatus.IN_PROGRESS, updated.status)
        assertEquals("moderator-1", updated.assigneeId)

        val message = transaction(database) { TicketMessagesTable.selectAll().single() }
        assertEquals(AuthorRole.SYSTEM, message[TicketMessagesTable.authorRole])
        assertEquals(MessageType.MESSAGE, message[TicketMessagesTable.type])
        assertTrue(message[TicketMessagesTable.body].contains("moderator-1"))
    }

    @Test
    fun `clarification request records moderator note`() {
        val created = repository.create(
            authorId = "author-1",
            req = CreateTicketRequest(subject = "Need help", body = "Fix issue")
        )

        repository.requestClarification(
            req = ClarifyRequest(ticketId = created.id, message = "Please clarify"),
            moderatorId = "mod-1"
        )

        val ticket = repository.list(TicketListRequest(status = TicketStatus.WAITING_USER)).single()
        assertEquals(TicketStatus.WAITING_USER, ticket.status)

        val message = transaction(database) { TicketMessagesTable.selectAll().toList().last() }
        assertEquals("mod-1", message[TicketMessagesTable.authorId])
        assertEquals(MessageType.CLARIFICATION_REQUEST, message[TicketMessagesTable.type])
    }

    @Test
    fun `applyModeration saves action and moderation note`() {
        val created = repository.create(
            authorId = "author-1",
            req = CreateTicketRequest(subject = "Report user", body = "Check user")
        )

        repository.applyModeration(
            req = ModerateRequest(
                ticketId = created.id,
                targetType = "comment",
                targetId = "comment-1",
                action = ModAction.HIDE,
                reason = "spam",
                note = "Hidden comment"
            ),
            moderatorId = "mod-1"
        )

        val action = transaction(database) { ModerationActionsTable.selectAll().single() }
        assertEquals("comment-1", action[ModerationActionsTable.targetId])
        assertEquals(ModAction.HIDE, action[ModerationActionsTable.action])

        val message = transaction(database) { TicketMessagesTable.selectAll().toList().last() }
        assertEquals(MessageType.MODERATION_NOTE, message[TicketMessagesTable.type])
    }

    @Test
    fun `answer updates status to answered`() {
        val created = repository.create(
            authorId = "author-1",
            req = CreateTicketRequest(subject = "Question", body = "Need answer")
        )

        repository.answer(
            req = AnswerRequest(ticketId = created.id, message = "Here you go"),
            moderatorId = "mod-1"
        )

        val ticket = repository.list(TicketListRequest(status = TicketStatus.ANSWERED)).single()
        assertEquals(TicketStatus.ANSWERED, ticket.status)
    }

    @Test
    fun `close sets status closed and closedAt`() {
        val created = repository.create(
            authorId = "author-1",
            req = CreateTicketRequest(subject = "Close me", body = "Done")
        )

        val closed = repository.close(
            req = CloseRequest(ticketId = created.id),
            actorId = "author-1"
        )

        assertEquals(TicketStatus.CLOSED, closed.status)
        assertNotNull(closed.closedAt)
    }

    @Test
    fun `rate records ticket rating`() {
        val created = repository.create(
            authorId = "author-1",
            req = CreateTicketRequest(subject = "Rate me", body = "Please rate")
        )

        repository.rate(
            req = RateRequest(ticketId = created.id, score = 5, comment = "Great"),
            userId = "author-1"
        )

        val rating = transaction(database) { TicketRatingsTable.selectAll().single() }
        assertEquals("Great", rating[TicketRatingsTable.comment])
    }

    @Test
    fun `list and messages return filtered data`() {
        val open = repository.create(
            authorId = "author-1",
            req = CreateTicketRequest(subject = "Open", body = "Still open")
        )
        val closed = repository.create(
            authorId = "author-2",
            req = CreateTicketRequest(subject = "Closed", body = "Will close")
        )
        repository.answer(AnswerRequest(ticketId = closed.id, message = "Resolved"), moderatorId = "mod-1")
        repository.close(CloseRequest(ticketId = closed.id), actorId = "author-2")

        val openTickets = repository.list(TicketListRequest(status = TicketStatus.OPEN))
        assertEquals(1, openTickets.size)
        assertEquals(open.id, openTickets.single().id)

        val messages = repository.messages(UUID.fromString(closed.id))
        assertTrue(messages.any { it.type == MessageType.ANSWER })
    }
}
