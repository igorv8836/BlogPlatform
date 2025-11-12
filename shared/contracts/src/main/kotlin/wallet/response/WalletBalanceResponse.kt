package wallet.response

import kotlinx.serialization.Serializable

@Serializable
data class WalletBalanceResponse(
    val currentBalance: Double
)