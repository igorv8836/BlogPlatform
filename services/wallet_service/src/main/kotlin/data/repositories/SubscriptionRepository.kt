package data.repositories

import com.example.utils.TimeUtils
import data.db.tables.SubscriptionsTable
import data.db.tables.SubscriptionsTable.authorId
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import wallet.SubscriptionStatus
import wallet.request.SubscribeToAuthorRequest
import wallet.response.CancelSubscriptionResponse
import wallet.response.SubscribeToAuthorResponse
import java.util.*


class SubscriptionRepository {
    fun subscribeToAuthor(userId: Long, request: SubscribeToAuthorRequest): SubscribeToAuthorResponse {
        return transaction {
            val subscriptionId = UUID.randomUUID()
            val now = TimeUtils.currentUtcOffsetDateTime()

            SubscriptionsTable.insert {
                it[id] = subscriptionId
                it[SubscriptionsTable.userId] = userId
                it[authorId] = request.authorId
                it[amount] = request.amount
                it[paymentMethodId] = UUID.fromString(request.paymentMethodId)
                it[startedAt] = now
                it[nextPaymentAt] = now.plusMonths(1)
            }

            SubscribeToAuthorResponse(
                subscriptionId = subscriptionId.toString(),
                userId = userId,
                authorId = request.authorId,
                amount = request.amount,
                status = SubscriptionStatus.ACTIVE,
                startedAt = now.toString()
            )
        }
    }

    fun cancelSubscription(subscriptionId: UUID, userId: Long): CancelSubscriptionResponse {
        return transaction {
            val now = TimeUtils.currentUtcOffsetDateTime()

            SubscriptionsTable
                .update({
                    (SubscriptionsTable.id eq subscriptionId) and (SubscriptionsTable.userId eq userId)
                }) {
                    it[status] = SubscriptionStatus.CANCELLED
                    it[cancelledAt] = now
                }

            val subscription = SubscriptionsTable.selectAll()
                .where { SubscriptionsTable.id eq subscriptionId }.firstOrNull()
                ?: throw Exception("Subscription not found or not owned by user")

            CancelSubscriptionResponse(
                subscriptionId = subscriptionId.toString(),
                userId = userId,
                authorId = subscription[authorId],
                cancelledAt = now.toString()
            )
        }
    }
}