package wallet.request

import kotlinx.serialization.Serializable

@Serializable
data class SubscribeToAuthorRequest(
    val authorId: Long,
    val amount: Double,
    val paymentMethodId: String
)