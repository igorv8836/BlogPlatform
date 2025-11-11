package posts.response

import kotlinx.serialization.Serializable

@Serializable
data class PostResponse(
    val id: String,
    val authorId: Long,
    val title: String,
    val content: String,
    val tags: List<String>,
    val status: String,
    val createdAt: String,
    val updatedAt: String?
)