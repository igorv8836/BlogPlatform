package wallet.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class WalletAsyncRequest {

    @Serializable
    @SerialName("walletCredit")
    class CreditRequest(
        val userId: Long,
        val creditedAmount: Double
    ) : WalletAsyncRequest()

    @Serializable
    @SerialName("walletDebit")
    class DebitRequest(
        val userId: Long,
        val debitedAmount: Double
    ) : WalletAsyncRequest()
}