package data.repositories

import com.example.utils.TimeUtils
import data.db.tables.WithdrawalRequestsTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import wallet.WithdrawalStatus
import wallet.request.RequestWithdrawalRequest
import wallet.response.RequestWithdrawalResponse
import java.util.*

class WithdrawalRequestRepository {
    fun requestWithdrawal(userId: Long, request: RequestWithdrawalRequest): RequestWithdrawalResponse {
        return transaction {
            val requestId = UUID.randomUUID()
            val now = TimeUtils.currentUtcOffsetDateTime()

            WithdrawalRequestsTable.insert {
                it[id] = requestId
                it[paymentMethodId] = UUID.fromString(request.paymentMethodId)
                it[WithdrawalRequestsTable.userId] = userId
                it[amount] = request.amount
                it[currency] = request.currency
                it[requestedAt] = now
            }

            RequestWithdrawalResponse(
                requestId = requestId.toString(),
                userId = userId,
                amount = request.amount,
                currency = request.currency,
                paymentMethodId = request.paymentMethodId,
                status = WithdrawalStatus.PENDING,
                requestedAt = now.toString()
            )
        }
    }
}