package posts.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePostRequest(
    val title: String?,
    val content: String?,
    val tags: List<String>?
)