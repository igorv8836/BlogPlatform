package wallet.response

import kotlinx.serialization.Serializable

@Serializable
data class CancelSubscriptionResponse(
    val subscriptionId: String,
    val userId: Long,
    val authorId: Long,
    val cancelledAt: String
)