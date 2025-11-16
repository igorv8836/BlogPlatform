package data.repositories

import com.example.utils.TimeUtils
import data.db.tables.HiddenAuthorsTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import posts.response.HideAuthorPostsResponse
import posts.response.RestoreAuthorPostsResponse


class HiddenAuthorRepository {

    fun hideAuthor(userId: Long, authorId: Long): HideAuthorPostsResponse {
        return transaction {
            val now = TimeUtils.currentUtcOffsetDateTime()

            HiddenAuthorsTable.insertIgnore {
                it[HiddenAuthorsTable.userId] = userId
                it[HiddenAuthorsTable.authorId] = authorId
                it[createdAt] = now
            }

            HideAuthorPostsResponse(
                userId = userId,
                authorId = authorId,
                timestamp = now.toString()
            )
        }
    }

    fun restoreAuthor(userId: Long, authorId: Long): RestoreAuthorPostsResponse {
        return transaction {
            val now = TimeUtils.currentUtcOffsetDateTime()

            val deletedCount = HiddenAuthorsTable.deleteWhere {
                (HiddenAuthorsTable.userId eq userId) and (HiddenAuthorsTable.authorId eq authorId)
            } == 1

            if (!deletedCount) throw BadRequestException("Author isn't hidden")

            RestoreAuthorPostsResponse(
                userId = userId,
                authorId = authorId,
                timestamp = now.toString()
            )
        }
    }

    fun getHiddenAuthorIdsForUser(userId: Long): List<Long> {
        return transaction {
            HiddenAuthorsTable
                .selectAll()
                .where { HiddenAuthorsTable.userId eq userId }
                .map { it[HiddenAuthorsTable.authorId] }
        }
    }
}