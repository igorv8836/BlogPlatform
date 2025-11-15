package notification.response

import kotlinx.serialization.Serializable
import users.UserRole
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class NotificationResponse(
    val notifications: List<Notification>,
)

@Serializable
data class Notification @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    val message: String,
    val createdAt: String,
    val type: String,
)