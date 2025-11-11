package users

enum class ReportReason{
    SPAM,
    HARASSMENT,
    HATE_SPEECH,
    VIOLENT_CONTENT,
    HARMFUL_MISINFORMATION,
    ILLEGAL_ACTIVITIES,
    UNKNOWN;

    fun toDbValue() = name.lowercase()
    companion object {
        fun fromString(value: String): ReportReason = when(value.lowercase()) {
            "spam" -> SPAM
            "harassment" -> HARASSMENT
            "hate_speech" -> HATE_SPEECH
            "violent_content" -> VIOLENT_CONTENT
            "harmful_misinformation" -> HARMFUL_MISINFORMATION
            "illegal_activities" -> ILLEGAL_ACTIVITIES
            "unknown" -> UNKNOWN
            else -> {
                UNKNOWN
            }
        }
    }
}

enum class UserRole {
    USER,
    MODERATOR;

    fun toDbValue() = name.lowercase()
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

    fun toDbValue() = name.lowercase()
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
