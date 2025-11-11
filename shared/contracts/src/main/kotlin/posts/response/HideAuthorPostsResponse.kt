package posts.response

import kotlinx.serialization.Serializable

@Serializable
data class HideAuthorPostsResponse(
    val userId: Long,
    val authorId: Long,
    val timestamp: String
)