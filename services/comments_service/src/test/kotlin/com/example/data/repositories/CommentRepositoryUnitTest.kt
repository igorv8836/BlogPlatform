package com.example.data.repositories

import io.ktor.server.plugins.*
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CommentRepositoryUnitTest {

    private val repository = CommentRepository()

    @Test
    fun `sanitizeMentions trims blank entries`() {
        val result = repository.sanitizeMentions(
            listOf(" user-a ", "", "user-b", " user-c ")
        )

        assertEquals(listOf("user-a", "user-b", "user-c"), result)
    }

    @Test
    fun `sanitizeMentions non-unique entries`() {
        val result = repository.sanitizeMentions(
            listOf(" user-a ", "", "user-a", " user-a       \t ")
        )

        assertEquals(listOf("user-a"), result)
    }

    @Test
    fun `sanitizeMentions returns empty list for null or blank-only input`() {
        assertTrue(repository.sanitizeMentions(null).isEmpty())
        assertTrue(repository.sanitizeMentions(listOf(" ", "\t", "")).isEmpty())
    }

    @Test
    fun `uuidEntityId returns entity id value for valid uuid`() {
        val raw = UUID.randomUUID().toString()

        assertEquals(UUID.fromString(raw), repository.uuidEntityId(raw).value)
    }

    @Test
    fun `uuidEntityId throws BadRequestException for invalid uuid`() {
        assertFailsWith<BadRequestException> { repository.uuidEntityId("not-a-uuid").value }
    }
}
