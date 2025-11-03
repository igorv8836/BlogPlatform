package comments.request

import kotlinx.serialization.Serializable

@Serializable
data class HideCommentRequest(
    val reason: String,
)
