package users.response

import kotlinx.serialization.Serializable
import users.UserRole

@Serializable
data class AuthResponse(
    val token: String
)

@Serializable
data class RecoveryResponse(
    val message: String,
)

@Serializable
data class UserResponse(
    val id: Long,
    val login: String,
    val creationDate: String,
    val role: UserRole,
    val desc: String,
    val avatar: String?
)

@Serializable
data class FollowersResponse(
    val followersId: List<Long>,
)