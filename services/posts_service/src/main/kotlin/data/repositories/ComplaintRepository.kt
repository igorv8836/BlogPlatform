package org.example.data.repositories

import com.example.utils.TimeUtils
import org.example.data.db.tables.ComplaintsTable
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import posts.request.FileComplaintOnPostRequest
import posts.response.FileComplaintOnPostResponse
import java.util.*

class ComplaintRepository {
    fun fileComplaintOnPost(
        postId: UUID,
        request: FileComplaintOnPostRequest,
        userId: Long
    ): FileComplaintOnPostResponse {
        return transaction {
            val now = TimeUtils.currentUtcOffsetDateTime()

            val complaintId = ComplaintsTable.insertAndGetId {
                it[ComplaintsTable.postId] = postId
                it[ComplaintsTable.complainantId] = UUID.randomUUID()
                it[ComplaintsTable.complainedById] = userId
                it[ComplaintsTable.reason] = request.reason
                it[ComplaintsTable.createdAt] = now
            }

            FileComplaintOnPostResponse(
                complaintId = complaintId.toString(),
                postId = postId.toString(),
                complainedById = userId.toString(),
                submittedAt = now.toString()
            )
        }
    }
}