package comments.request

import kotlinx.serialization.Serializable

@Serializable
data class EditCommentRequest(
    val body: String,
    val mentions: List<String>? = null,
)
