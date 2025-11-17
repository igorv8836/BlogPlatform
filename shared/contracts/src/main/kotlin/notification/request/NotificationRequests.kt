package notification.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import notification.NotificationType

@Serializable
sealed class NotificationAsyncRequest {
    @Serializable
    @SerialName("create")
    data class CreateNotificationRequest(
        val targetUserId: Long?,
        val message: String,
        val notificationType: NotificationType
    ) : NotificationAsyncRequest()

    @Serializable
    @SerialName("notifySubscribers")
    data class NotifySubscribersNotificationRequest(
        val authorUserId: Long,
        val token: String,
        val message: String,
        val notificationType: NotificationType
    ) : NotificationAsyncRequest()
}
