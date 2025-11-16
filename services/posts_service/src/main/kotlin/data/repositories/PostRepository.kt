package data.repositories

import com.example.utils.TimeUtils
import data.db.tables.PostStatus
import data.db.tables.PostsTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import posts.request.CreatePostRequest
import posts.request.UpdatePostRequest
import posts.response.DeletePostResponse
import posts.response.PostResponse
import posts.response.PostSummary
import posts.response.SearchPostsResponse
import java.util.*
import kotlin.time.ExperimentalTime

class PostRepository {
    fun createPost(post: CreatePostRequest, userId: Long): PostResponse? {
        return transaction {
            val id = UUID.randomUUID()
            val now = TimeUtils.currentUtcOffsetDateTime()
            PostsTable.insert {
                it[this.id] = id
                it[title] = post.title
                it[content] = post.content
                it[authorId] = userId
                it[tags] = post.tags ?: emptyList()
                it[status] = PostStatus.PUBLISHED
                it[createdAt] = now
                it[updatedAt] = null
            }
            findById(id)
        }
    }

    fun getPostById(postId: UUID): PostResponse? {
        return transaction { findById(postId) }
    }

    fun updatePost(postId: UUID, update: UpdatePostRequest, userId: Long): PostResponse? {
        return transaction {

            checkIfExistsAndOwned(postId, userId)

            val now = TimeUtils.currentUtcOffsetDateTime()

            PostsTable.update({ PostsTable.id eq postId }) { postRow ->
                update.title?.let { postRow[PostsTable.title] = it }
                update.content?.let { postRow[PostsTable.content] = it }
                update.tags?.let { postRow[PostsTable.tags] = it }
                postRow[PostsTable.updatedAt] = now
            }

            findById(postId)

        }
    }

    fun deletePost(postId: UUID, userId: Long): DeletePostResponse {
        return transaction {

            checkIfExistsAndOwned(postId, userId)

            val now = TimeUtils.currentUtcOffsetDateTime()

            // Mark post as DELETED
            PostsTable.update({ PostsTable.id eq postId }) {
                it[status] = PostStatus.DELETED
                it[updatedAt] = now
            }

            DeletePostResponse(
                id = postId.toString(),
                deletedAt = now.toString()
            )
        }
    }

    fun searchPosts(
        query: String?,
        authorId: Long?,
        status: String?,
        page: Int,
        size: Int
    ): SearchPostsResponse {
        return transaction {
            var select = PostsTable.selectAll()

            if (query != null) {
                select = select.where { PostsTable.title like "%$query%" or (PostsTable.content like "%$query%") }
            }

            if (authorId != null) {
                select = select.andWhere { PostsTable.authorId eq authorId }
            }

            if (status != null) {
                select = select.andWhere { PostsTable.status eq PostStatus.valueOf(status) }
            }

            val totalCount = select.count()
            val posts = select
                .limit(size)
                .map { row ->
                    PostSummary(
                        id = row[PostsTable.id].toString(),
                        authorId = row[PostsTable.authorId],
                        title = row[PostsTable.title],
                        tags = row[PostsTable.tags],
                        status = row[PostsTable.status].name,
                        createdAt = row[PostsTable.createdAt].toString()
                    )
                }

            SearchPostsResponse(
                posts = posts,
                total = totalCount.toInt(),
                page = page,
                size = size
            )
        }
    }

    private fun findById(id: UUID): PostResponse? {
        return PostsTable
            .selectAll()
            .where(PostsTable.id eq id)
            .firstOrNull()
            ?.let { toResponse(it) }
    }

    private fun checkIfExistsAndOwned(postId: UUID, userId: Long) {
        val existingPost = PostsTable
            .selectAll()
            .where(PostsTable.id eq postId)
            .firstOrNull()

        if (existingPost == null) {
            throw BadRequestException("Post not exists")
        }

        if (existingPost[PostsTable.authorId] != userId) {
            throw BadRequestException("userId not equals authorId")
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun toResponse(row: ResultRow): PostResponse {
        return PostResponse(
            id = row[PostsTable.id].toString(),
            authorId = row[PostsTable.authorId],
            title = row[PostsTable.title],
            content = row[PostsTable.content],
            tags = row[PostsTable.tags],
            status = row[PostsTable.status].name,
            createdAt = row[PostsTable.createdAt].toString(),
            updatedAt = row[PostsTable.updatedAt]?.toString()
        )
    }
}