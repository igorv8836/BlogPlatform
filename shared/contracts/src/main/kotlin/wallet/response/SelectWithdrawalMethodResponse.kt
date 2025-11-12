package wallet.response

import kotlinx.serialization.Serializable

@Serializable
data class SelectWithdrawalMethodResponse(
    val userId: Long,
    val selectedMethodId: String,
    val selectedAt: String
)