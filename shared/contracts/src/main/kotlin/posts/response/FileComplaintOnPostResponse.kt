package posts.response

import kotlinx.serialization.Serializable

@Serializable
data class FileComplaintOnPostResponse(
    val complaintId: String,
    val postId: String,
    val complainedById: String,
    val submittedAt: String
)