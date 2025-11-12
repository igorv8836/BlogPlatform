package wallet.response

import kotlinx.serialization.Serializable

@Serializable
data class DeletePaymentMethodResponse(
    val paymentMethodId: String,
    val userId: Long,
    val deletedAt: String
)