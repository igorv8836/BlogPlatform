package wallet.response

import kotlinx.serialization.Serializable
import wallet.PaymentType

@Serializable
data class AddPaymentMethodResponse(
    val paymentMethodId: String,
    val userId: Long,
    val type: PaymentType,
    val maskedDetails: String,
    val isDefault: Boolean,
    val addedAt: String
)