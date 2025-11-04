package com.example.data.repositories

import com.example.data.db.tables.*
import comments.ReactionType
import comments.request.CreateCommentRequest
import comments.request.EditCommentRequest
import comments.request.TargetType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.*

class CommentRepositoryTest {

    private lateinit var database: Database
    private lateinit var commentRepository: CommentRepository
    private lateinit var complaintRepository: ComplaintRepository
    private lateinit var reactionRepository: ReactionRepository

    @BeforeTest
    fun setUp() {
        database = Database.connect(
            url = "jdbc:h2:mem:comments_${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        transaction(database) {
            SchemaUtils.create(
                CommentsTable,
                CommentEditsTable,
                MentionsTable,
                ModerationLogsTable,
                ComplaintsTable,
                ReactionsTable,
            )
        }
        commentRepository = CommentRepository()
        complaintRepository = ComplaintRepository()
        reactionRepository = ReactionRepository()
    }

    @Test
    fun `create comment persists a fully populated record`() {
        val response = commentRepository.create(
            authorId = "author-1",
            req = CreateCommentRequest(
                targetType = TargetType.Post,
                targetId = "post-1",
                body = "First!",
                mentions = listOf(" user-a ", "user-b", "user-a")
            )
        )

        assertNotNull(response)
        assertEquals("author-1", response.authorId)
        assertEquals("post", response.targetType)
        assertEquals("post-1", response.targetId)
        assertEquals(listOf("user-a", "user-b"), response.mentions)
        assertFalse(response.edited)
        assertFalse(response.isDeleted)
        assertFalse(response.isHidden)

        val stored = transaction(database) { CommentsTable.selectAll().single() }
        assertEquals("First!", stored[CommentsTable.body])
    }

    @Test
    fun `edit comment updates body marks edited and replaces mentions`() {
        val created = commentRepository.create(
            authorId = "author-1",
            req = CreateCommentRequest(
                targetType = TargetType.Post,
                targetId = "post-1",
                body = "Original",
                mentions = listOf("user-a")
            )
        )
        val commentId = UUID.fromString(requireNotNull(created).id)

        val updated = commentRepository.edit(
            commentId = commentId,
            editorId = "author-1",
            req = EditCommentRequest(body = "Updated text", mentions = listOf("user-b", "user-b", " user-c "))
        )

        assertNotNull(updated)
        assertEquals("Updated text", updated.body)
        assertTrue(updated.edited)
        assertEquals(listOf("user-b", "user-c"), updated.mentions)

        val auditRow = transaction(database) { CommentEditsTable.selectAll().single() }
        assertEquals("Original", auditRow[CommentEditsTable.oldBody])
        assertEquals("Updated text", auditRow[CommentEditsTable.newBody])
    }

    @Test
    fun `softDelete marks comment and records moderation entry`() {
        val created = commentRepository.create(
            authorId = "author-1",
            req = CreateCommentRequest(
                targetType = TargetType.Post,
                targetId = "post-1",
                body = "To delete"
            )
        )
        val commentId = UUID.fromString(requireNotNull(created).id)

        commentRepository.softDelete(commentId, actorId = "author-1")

        val row = transaction(database) { CommentsTable.selectAll().single() }
        assertTrue(row[CommentsTable.isDeleted])
        val log = transaction(database) { ModerationLogsTable.selectAll().single() }
        assertEquals("delete", log[ModerationLogsTable.action])
        assertEquals("author-1", log[ModerationLogsTable.actorId])
    }

    @Test
    fun `hide and restore toggle visibility`() {
        val created = commentRepository.create(
            authorId = "author-1",
            req = CreateCommentRequest(
                targetType = TargetType.Post,
                targetId = "post-1",
                body = "Visibility test"
            )
        )
        val commentId = UUID.fromString(requireNotNull(created).id)

        commentRepository.hide(commentId, moderatorId = "mod-1", reason = "spam")

        var stored = transaction(database) { CommentsTable.selectAll().single() }
        assertTrue(stored[CommentsTable.isHidden])
        assertEquals("mod-1", stored[CommentsTable.hiddenBy])
        assertEquals("spam", stored[CommentsTable.hiddenReason])

        commentRepository.restore(commentId, moderatorId = "mod-1")

        stored = transaction(database) { CommentsTable.selectAll().single() }
        assertFalse(stored[CommentsTable.isHidden])
        assertNull(stored[CommentsTable.hiddenReason])

        val actions = transaction(database) { ModerationLogsTable.selectAll().map { it[ModerationLogsTable.action] } }
        assertEquals(listOf("hide", "restore"), actions)
    }

    @Test
    fun `pin and unpin change pinned timestamp`() {
        val created = commentRepository.create(
            authorId = "author-1",
            req = CreateCommentRequest(
                targetType = TargetType.Post,
                targetId = "post-1",
                body = "Pin me"
            )
        )
        val commentId = UUID.fromString(requireNotNull(created).id)

        commentRepository.pin(commentId, authorId = "author-1")
        var stored = transaction(database) { CommentsTable.selectAll().single() }
        assertNotNull(stored[CommentsTable.pinnedByAuthorAt])

        commentRepository.unpin(commentId, authorId = "author-1")
        stored = transaction(database) { CommentsTable.selectAll().single() }
        assertNull(stored[CommentsTable.pinnedByAuthorAt])
    }

    @Test
    fun `list returns newest comments first`() {
        repeat(3) {
            commentRepository.create(
                authorId = "author-$it",
                req = CreateCommentRequest(
                    targetType = TargetType.Post,
                    targetId = "post-42",
                    body = "Comment $it"
                )
            )
        }

        val responses = commentRepository.list(TargetType.Post, targetId = "post-42", limit = 10, offset = 0)

        assertEquals(3, responses.size)
        assertEquals("Comment 2", responses.first().body)
        assertEquals("Comment 0", responses.last().body)
    }

    @Test
    fun `complaint repository stores complaint`() {
        val created = commentRepository.create(
            authorId = "author-1",
            req = CreateCommentRequest(
                targetType = TargetType.Post,
                targetId = "post-1",
                body = "Report me"
            )
        )
        val commentId = UUID.fromString(requireNotNull(created).id)

        complaintRepository.create(commentId, userId = "reporter", reason = "abuse")

        val stored = transaction(database) { ComplaintsTable.selectAll().single() }
        assertEquals("reporter", stored[ComplaintsTable.complainantId])
        assertEquals("abuse", stored[ComplaintsTable.reason])
    }

    @Test
    fun `reaction repository set and remove maintain reactions`() {
        val created = commentRepository.create(
            authorId = "author-1",
            req = CreateCommentRequest(
                targetType = TargetType.Post,
                targetId = "post-1",
                body = "Great post"
            )
        )
        val commentId = UUID.fromString(requireNotNull(created).id)

        reactionRepository.set(commentId, userId = "user-1", type = ReactionType.like)

        var reactions = transaction(database) { ReactionsTable.selectAll().toList() }
        assertEquals(1, reactions.size)
        assertEquals("user-1", reactions.single()[ReactionsTable.userId])

        reactionRepository.remove(commentId, userId = "user-1", type = ReactionType.like)

        reactions = transaction(database) { ReactionsTable.selectAll().toList() }
        assertTrue(reactions.isEmpty())
    }
}
