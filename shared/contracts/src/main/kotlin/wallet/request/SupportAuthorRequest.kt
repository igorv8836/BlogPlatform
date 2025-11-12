package wallet.request

import kotlinx.serialization.Serializable

@Serializable
data class SupportAuthorRequest(
    val authorId: Long,
    val amount: Double,
    val paymentMethodId: String
)