package payment.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import wallet.Currency

@Serializable
sealed class PaymentAsyncRequest {

    @Serializable
    @SerialName("creditFund")
    data class CreditFundsRequest(
        val userId: Long,
        val amount: Double,
        val currency: Currency,
        val sourceCreditId: String
    ) : PaymentAsyncRequest()

    @Serializable
    @SerialName("debitFund")
    data class DebitFundsRequest(
        val userId: Long,
        val amount: Double,
        val currency: Currency,
        val sourceDebitId: String
    ) : PaymentAsyncRequest()

    @Serializable
    @SerialName("transferFund")
    data class TransferFundsRequest(
        val fromUserId: Long,
        val toUserId: Long,
        val currency: Currency,
        val amount: Double
    ) : PaymentAsyncRequest()
}