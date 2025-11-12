package wallet.request

import kotlinx.serialization.Serializable
import wallet.PaymentType

@Serializable
data class AddPaymentMethodRequest(
    val type: PaymentType,
    val details: Map<String, String>
)