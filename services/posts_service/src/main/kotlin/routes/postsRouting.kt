package org.example.routes

import com.example.utils.userIdOrNull
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.example.data.repositories.ComplaintRepository
import org.example.data.repositories.HiddenAuthorRepository
import org.example.data.repositories.PostRepository
import org.koin.ktor.ext.inject
import posts.request.CreatePostRequest
import posts.request.FileComplaintOnPostRequest
import posts.request.UpdatePostRequest
import java.util.*

fun Application.configurePostsRouting() {
    val postRepository by inject<PostRepository>()
    val complaintRepository by inject<ComplaintRepository>()
    val hiddenAuthorRepository by inject<HiddenAuthorRepository>()

    routing {
        route("/api/v1/posts") {
            // Create a new post
            post {
                val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                val request = call.receive<CreatePostRequest>()
                val response = postRepository.createPost(request, userId)
                if (response != null) {
                    call.respond(response)
                } else {
                    call.respond(io.ktor.http.HttpStatusCode.InternalServerError)
                }
            }

            // Update post
            patch {
                val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                val postId = UUID.fromString(call.parameters["postId"])
                val request = call.receive<UpdatePostRequest>()
                val response = postRepository.updatePost(postId, request, userId)
                if (response != null) {
                    call.respond(response)
                } else {
                    call.respond(io.ktor.http.HttpStatusCode.NotFound)
                }
            }

            // Delete post
            delete {
                val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                val postId = UUID.fromString(call.parameters["postId"])
                val response = postRepository.deletePost(postId, userId)
                call.respond(response)
            }

            // File complaint on post
            post("/complaints") {
                val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                val postId = UUID.fromString(call.parameters["postId"])
                    ?: throw IllegalArgumentException("Post ID required")
                val request = call.receive<FileComplaintOnPostRequest>()
                val response = complaintRepository.fileComplaintOnPost(postId, request, userId)
                call.respond(response)
            }

            // Search posts
            get("/search") {
                val query = call.request.queryParameters["query"]
                val authorId = call.request.queryParameters["authorId"]?.toLongOrNull()
                val status = call.request.queryParameters["status"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 10

                val response = postRepository.searchPosts(
                    query = query,
                    authorId = authorId,
                    status = status,
                    page = page,
                    size = size
                )
                call.respond(response)
            }

            route("/hidden") {
                post {
                    val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                    val authorId: Long = call.request.queryParameters["authorId"]?.toLongOrNull()
                        ?: throw BadRequestException("authorId is Null")

                    val response = hiddenAuthorRepository.hideAuthor(userId, authorId)
                    call.respond(response)
                }

                delete {
                    val userId: Long = call.userIdOrNull()?.toLong() ?: 100
                    val authorId: Long = call.request.queryParameters["authorId"]?.toLongOrNull()
                        ?: throw BadRequestException("authorId is Null")

                    val response = hiddenAuthorRepository.restoreAuthor(userId, authorId)
                    call.respond(response)
                }
            }

            // Get post by ID
            get {
                val postId = UUID.fromString(call.parameters["postId"])
                val response = postRepository.getPostById(postId)
                if (response != null) {
                    call.respond(response)
                } else {
                    call.respond(io.ktor.http.HttpStatusCode.NotFound)
                }
            }
        }
    }
}