package security

data class TokenConfig(
    val issuer: String,
    val audience: String,
    val expiresIn: Long,
    val secretKey: String,
)

data class TokenClaim(
    val name: String,
    val value: String,
)