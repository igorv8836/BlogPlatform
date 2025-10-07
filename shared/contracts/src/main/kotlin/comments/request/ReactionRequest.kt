package comments.request

import comments.ReactionType
import kotlinx.serialization.Serializable

@Serializable
data class ReactionRequest(val type: ReactionType)