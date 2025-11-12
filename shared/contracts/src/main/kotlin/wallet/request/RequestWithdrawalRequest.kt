package wallet.request

import kotlinx.serialization.Serializable
import wallet.Currency

@Serializable
data class RequestWithdrawalRequest(
    val amount: Double,
    val currency: Currency,
    val paymentMethodId: String
)