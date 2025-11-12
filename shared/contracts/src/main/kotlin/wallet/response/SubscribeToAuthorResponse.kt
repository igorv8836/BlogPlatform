package wallet.response

import kotlinx.serialization.Serializable
import wallet.SubscriptionStatus

@Serializable
data class SubscribeToAuthorResponse(
    val subscriptionId: String,
    val userId: Long,
    val authorId: Long,
    val amount: Double,
    val status: SubscriptionStatus,
    val startedAt: String
)