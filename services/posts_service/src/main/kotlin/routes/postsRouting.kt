package routes

import clients.UsersServiceClient
import com.example.ClientConfig
import com.example.clients.SupportServiceClient
import com.example.createServiceHttpClient
import com.example.utils.tokenOrNull
import com.example.utils.userIdOrNull
import data.repositories.HiddenAuthorRepository
import data.repositories.PostRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import posts.request.ComplaintRequest
import posts.request.CreatePostRequest
import posts.request.UpdatePostRequest
import java.util.*

fun Application.configurePostsRouting() {
    val postRepository by inject<PostRepository>()
    val hiddenAuthorRepository by inject<HiddenAuthorRepository>()

    val usersServiceClient by inject<UsersServiceClient>()
    val supportServiceClient by inject<SupportServiceClient>()

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

            route("/follow") {
                post {
                    val authorId: Long = call.request.queryParameters["authorId"]?.toLongOrNull()
                        ?: throw BadRequestException("authorId is Null")
                    val token: String = call.tokenOrNull() ?: throw BadRequestException("token is null")

                    val response: HttpStatusCode = UsersServiceClient(
                        createServiceHttpClient(ClientConfig(baseUrl = "http://0.0.0.0:8085"))
                    ).followAuthor(
                        authorId = authorId,
                        jwtToken = token
                    )

                    call.respond(response)
                }
                delete {
                    val authorId: Long = call.request.queryParameters["authorId"]?.toLongOrNull()
                        ?: throw BadRequestException("authorId is Null")
                    val token: String = call.tokenOrNull() ?: throw BadRequestException("token is null")

                    val response: HttpStatusCode = usersServiceClient.unfollowAuthor(
                        authorId = authorId,
                        jwtToken = token
                    )

                    call.respond(response)
                }
            }

            route("/complaint") {
                post("/post") {
                    val request = call.receive<ComplaintRequest>()
                    val token: String = call.tokenOrNull() ?: throw BadRequestException("token is null")
                    val postId: String = call.request.queryParameters["postId"]
                        ?: throw BadRequestException("postId is Null")

                    val response = supportServiceClient.complaint(
                        subject = "Complaint on post with id=$postId",
                        body = request.reason,
                        jwtToken = token
                    )

                    call.respond(response)
                }
                post("/user") {
                    val request = call.receive<ComplaintRequest>()
                    val token: String = call.tokenOrNull() ?: throw BadRequestException("token is null")
                    val userId: Long = call.request.queryParameters["userId"]?.toLongOrNull()
                        ?: throw BadRequestException("userId is Null")

                    val response = supportServiceClient.complaint(
                        subject = "Complaint on user with id=$userId",
                        body = request.reason,
                        jwtToken = token
                    )

                    call.respond(response)
                }
            }
        }
    }
}