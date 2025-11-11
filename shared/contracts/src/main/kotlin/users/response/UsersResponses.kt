package users.response

import kotlinx.serialization.Serializable
import users.UserRole

@Serializable
data class AuthResponse(
    val token: String
)

//TODO Should think about protection of account. Maybe add some recovery question or something else
@Serializable
data class RecoveryResponse(
    val message: String,
)

@Serializable
data class UserResponse(
    val id: Long,
    val login: String,
    val creationDate: String, //TODO change to Date
    val role: UserRole,
    val desc: String,
    val avatar: String? //TODO DO NOT forget to change this to an image
)