package notification.request

import kotlinx.serialization.Serializable

@Serializable
data class NotificationRequest(
    val message: String,
    val type: String,
)
