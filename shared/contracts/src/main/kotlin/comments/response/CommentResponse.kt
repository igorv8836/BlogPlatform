package comments.response

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CommentResponse(
    val id: String,
    val targetType: String,
    val targetId: String,
    val parentId: String?,
    val authorId: String,
    val body: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val edited: Boolean,
    val isDeleted: Boolean,
    val isHidden: Boolean,
    val pinnedByAuthorAt: Instant?
)