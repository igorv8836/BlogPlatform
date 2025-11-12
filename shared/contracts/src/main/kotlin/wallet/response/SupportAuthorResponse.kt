package wallet.response

import kotlinx.serialization.Serializable

@Serializable
data class SupportAuthorResponse(
    val supportId: String,
    val userId: Long,
    val authorId: Long,
    val amount: Double,
    val supportedAt: String
)