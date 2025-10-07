package comments.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentRequest(
    val targetType: String,
    val targetId: String,
    val parentId: String? = null,
    val body: String
)