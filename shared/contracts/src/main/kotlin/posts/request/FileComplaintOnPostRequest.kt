package posts.request

import kotlinx.serialization.Serializable

@Serializable
data class FileComplaintOnPostRequest(
    val reason: String
)