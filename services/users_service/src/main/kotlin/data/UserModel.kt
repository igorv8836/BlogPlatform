package data

import users.UserRole
import java.time.OffsetDateTime

data class UserModel(
    val id: Long,
    val login: String,
    val email: String,
    val hashPassword: String,
    val role: UserRole,
    val creationDate: OffsetDateTime,
    val salt: String,
    val avatar: String,
    val desc: String
)