package users

enum class UserRole {
    USER,
    MODERATOR;

    companion object {
        fun fromString(value: String): UserRole = when(value.lowercase()) {
            "user" -> USER
            "moderator" -> MODERATOR
            else -> {
                USER
            }
        }
    }
}

enum class UserStatus {
    BANNED,
    ACTIVE,
    SUSPENDED;

    companion object {
        fun fromString(value: String): UserStatus = when(value.lowercase()) {
            "active" -> ACTIVE
            "banned" -> BANNED
            "suspend" -> SUSPENDED
            else -> {
                ACTIVE
            }
        }
    }
}
