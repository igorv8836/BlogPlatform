package com.example.data.repositories

import data.db.tables.HiddenAuthorsTable
import data.db.tables.PostStatus
import data.db.tables.PostsTable
import data.db.tables.PostsTable.status
import data.repositories.PostRepository
import io.ktor.server.plugins.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import posts.request.CreatePostRequest
import posts.request.UpdatePostRequest
import java.util.*
import kotlin.test.*

private fun createTestDatabase(): Database {
    val db = Database.connect(
        url = "jdbc:h2:mem:posts_${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )
    transaction(db) {
        SchemaUtils.create(
            PostsTable,
            HiddenAuthorsTable
        )
    }
    return db
}

class PostRepositoryTest {

    private lateinit var database: Database
    private lateinit var postRepository: PostRepository

    @BeforeTest
    fun setUp() {
        database = createTestDatabase()
        postRepository = PostRepository()
    }

    @AfterTest
    fun tearDown() {
        transaction(database) {
            PostsTable.deleteAll()
            HiddenAuthorsTable.deleteAll()
        }
    }

    @Test
    fun `createPost should insert a new post record and return PostResponse`() {
        val userId = 100L
        val request = CreatePostRequest(
            title = "My First Post",
            content = "This is the content of my post.",
            tags = listOf("tech", "blog")
        )

        val response = postRepository.createPost(request, userId)

        assertNotNull(response)
        assertEquals(request.title, response.title)
        assertEquals(request.content, response.content)
        assertEquals(request.tags, response.tags)
        assertEquals(userId, response.authorId)

        val stored = transaction(database) {
            PostsTable.selectAll().where { PostsTable.authorId eq userId }.singleOrNull()
        }
        assertNotNull(stored)
        assertEquals(request.title, stored[PostsTable.title])
        assertEquals(request.content, stored[PostsTable.content])
        assertEquals(userId, stored[PostsTable.authorId])
    }

    @Test
    fun `updatePost should modify an existing post`() {
        val userId = 100L
        val postId = UUID.randomUUID()
        val initialRequest =
            CreatePostRequest(title = "Old Title", content = "Old Content", tags = emptyList())
        
        transaction(database) {
            PostsTable.insert {
                it[id] = EntityID(postId, PostsTable)
                it[title] = initialRequest.title
                it[content] = initialRequest.content
                it[authorId] = userId
            }
        }

        val updateRequest = UpdatePostRequest(title = "New Title", content = "New Content", tags = null)

        val response = postRepository.updatePost(postId, updateRequest, userId)

        assertNotNull(response)
        assertEquals(updateRequest.title, response.title)
        assertEquals(updateRequest.content, response.content)

        val stored = transaction(database) {
            PostsTable.selectAll().where { PostsTable.id eq postId }.single()
        }
        assertEquals(updateRequest.title, stored[PostsTable.title])
        assertEquals(updateRequest.content, stored[PostsTable.content])
    }

    @Test
    fun `updatePost should fail if post does not exist`() {
        val userId = 100L
        val nonExistentPostId = UUID.randomUUID()
        val updateRequest = UpdatePostRequest(title = "New Title", content = "New Content", tags = null)

        assertFailsWith<BadRequestException> {
            postRepository.updatePost(nonExistentPostId, updateRequest, userId)
        }
    }

    @Test
    fun `updatePost should fail if user is not the author`() {
        val originalAuthorId = 100L
        val otherUserId = 200L
        val postId = UUID.randomUUID()
        val initialRequest =
            CreatePostRequest(title = "Old Title", content = "Old Content", tags = emptyList())
        transaction(database) {
            PostsTable.insert {
                it[id] = EntityID(postId, PostsTable)
                it[title] = initialRequest.title
                it[content] = initialRequest.content
                it[status] = PostStatus.PUBLISHED
                it[authorId] = originalAuthorId
            }
        }

        val updateRequest = UpdatePostRequest(title = "New Title", content = "New Content", tags = null)

        assertFailsWith<BadRequestException> {
            postRepository.updatePost(postId, updateRequest, otherUserId)
        }
    }

    @Test
    fun `deletePost should mark post as deleted and return DeletePostResponse`() {
        val userId = 100L
        val postId = UUID.randomUUID()
        val initialRequest =
            CreatePostRequest(title = "Old Title", content = "Old Content", tags = emptyList())
        transaction(database) {
            PostsTable.insert {
                it[id] = EntityID(postId, PostsTable)
                it[title] = initialRequest.title
                it[content] = initialRequest.content
                it[status] = PostStatus.PUBLISHED
                it[authorId] = userId
            }
        }

        val response = postRepository.deletePost(postId, userId)

        assertNotNull(response)
        assertEquals(postId.toString(), response.id)

        val stored = transaction(database) {
            PostsTable.selectAll().where { PostsTable.id eq postId }.single()
        }
        assertEquals(stored[status], PostStatus.DELETED)
    }

    @Test
    fun `getPostById should return a post if it exists and is not deleted`() {
        val userId = 100L
        val postId = UUID.randomUUID()
        val initialRequest =
            CreatePostRequest(title = "Old Title", content = "Old Content", tags = emptyList())
        transaction(database) {
            PostsTable.insert {
                it[id] = EntityID(postId, PostsTable)
                it[title] = initialRequest.title
                it[content] = initialRequest.content
                it[status] = PostStatus.PUBLISHED
                it[authorId] = userId
            }
        }

        val response = postRepository.getPostById(postId)

        assertNotNull(response)
        assertEquals(initialRequest.title, response.title)
    }

    @Test
    fun `getPostById should return null if post is deleted`() {
        val userId = 100L
        val postId = UUID.randomUUID()
        val initialRequest =
            CreatePostRequest(title = "Old Title", content = "Old Content", tags = emptyList())
        transaction(database) {
            PostsTable.insert {
                it[id] = EntityID(postId, PostsTable)
                it[title] = initialRequest.title
                it[content] = initialRequest.content
                it[status] = PostStatus.DELETED
                it[authorId] = userId
            }
        }

        val response = postRepository.getPostById(postId)

        assertNull(response)
    }

    @Test
    fun `searchPosts should return posts matching criteria`() {
        val authorId = 200L
        transaction(database) {
            PostsTable.insert {
                it[title] = "Post 1"
                it[content] = "Content about tech"
                it[status] = PostStatus.PUBLISHED
                it[PostsTable.authorId] = authorId
            }
            PostsTable.insert {
                it[title] = "Post 2"
                it[content] = "Content about sports"
                it[status] = PostStatus.PUBLISHED
                it[PostsTable.authorId] = authorId
            }
            PostsTable.insert {
                it[title] = "Draft Post"
                it[content] = "Draft content"
                it[status] = PostStatus.PUBLISHED
                it[PostsTable.authorId] = authorId
            }
        }

        val results =
            postRepository.searchPosts(query = "tech", authorId = authorId, status = "PUBLISHED", page = 1, size = 10)

        assertEquals(1, results.posts.size)
        assertEquals("Post 1", results.posts.first().title)
    }
}