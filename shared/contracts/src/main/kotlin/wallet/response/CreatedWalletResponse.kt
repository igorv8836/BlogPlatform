package wallet.response

import kotlinx.serialization.Serializable

@Serializable
data class CreatedWalletResponse(
    val userId: Long,
    val balance: Double,
    val currency: String,
    val createdAt: String
)