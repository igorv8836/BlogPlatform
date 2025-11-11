package posts.response

import kotlinx.serialization.Serializable

@Serializable
data class SearchPostsResponse(
    val posts: List<PostSummary>,
    val total: Int,
    val page: Int,
    val size: Int
)

@Serializable
data class PostSummary(
    val id: String,
    val authorId: Long,
    val title: String,
    val status: String,
    val createdAt: String,
    val tags: List<String>
)