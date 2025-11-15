package users.request

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val login: String,
    val email: String,
    val password: String,
    val desc: String,
    val avatar: String?, //TODO change this to image (And also define what library will be used)
)

@Serializable
data class AuthRequest(
    val login: String,
    val password: String,
)

@Serializable
data class RecoveryRequest(
    val login: String,
)

@Serializable
data class BanRequest(
    val duration: Int, //Number of days
    val message: String,
)

//TODO Unsure about nullable type. Should change this request
@Serializable
data class UpdateProfileRequest(
    val email: String? = null,
    val password: String? = null,
    val desc: String? = null,
    val avatar: String? = null,
)
