package comments.request

import kotlinx.serialization.Serializable

@Serializable
data class ComplaintRequest(val reason: String)