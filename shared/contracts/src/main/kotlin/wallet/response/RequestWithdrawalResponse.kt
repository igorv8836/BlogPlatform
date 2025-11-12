package wallet.response

import kotlinx.serialization.Serializable
import wallet.Currency
import wallet.WithdrawalStatus

@Serializable
data class RequestWithdrawalResponse(
    val requestId: String,
    val userId: Long,
    val amount: Double,
    val currency: Currency,
    val paymentMethodId: String,
    val status: WithdrawalStatus,
    val requestedAt: String
)