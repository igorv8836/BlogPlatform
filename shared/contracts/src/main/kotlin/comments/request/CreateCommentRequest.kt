package comments.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentRequest(
    val targetType: TargetType = TargetType.Post,
    val targetId: String,
    val parentId: String? = null,
    val body: String,
    val mentions: List<String> = emptyList(),
)

enum class TargetType(val value: String) {
    Post("post")
}
