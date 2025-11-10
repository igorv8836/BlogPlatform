package posts.response

import kotlinx.serialization.Serializable

@Serializable
data class DeletePostResponse(
    val id: String,
    val deletedAt: String
)