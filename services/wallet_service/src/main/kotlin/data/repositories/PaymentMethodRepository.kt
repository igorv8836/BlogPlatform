package data.repositories

import com.example.utils.TimeUtils
import data.db.tables.PaymentMethodsTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import wallet.request.AddPaymentMethodRequest
import wallet.response.AddPaymentMethodResponse
import wallet.response.DeletePaymentMethodResponse
import wallet.response.SelectWithdrawalMethodResponse
import java.util.*


class PaymentMethodRepository {
    fun addPaymentMethod(userId:Long, request: AddPaymentMethodRequest): AddPaymentMethodResponse {
        return transaction {
            val methodId = UUID.randomUUID()
            val now = TimeUtils.currentUtcOffsetDateTime()

            val existingMethodsCount =
                PaymentMethodsTable.selectAll().where { PaymentMethodsTable.userId eq userId }.count()
            val isDefault = existingMethodsCount.toInt() == 0

            PaymentMethodsTable.insert {
                it[id] = methodId
                it[PaymentMethodsTable.userId] = userId
                it[type] = request.type
                it[details] = mapToString(request.details)
                it[maskedDetails] =
                    request.details["number"]?.let { num -> "****${num.takeLast(4)}" } ?: "N/A"
                it[PaymentMethodsTable.isDefault] = isDefault
                it[createdAt] = now
                it[updatedAt] = now
            }

            AddPaymentMethodResponse(
                paymentMethodId = methodId.toString(),
                userId = userId,
                type = request.type,
                maskedDetails = request.details["number"]?.let { num -> "****${num.takeLast(4)}" } ?: "N/A",
                isDefault = isDefault,
                addedAt = now.toString()
            )
        }
    }

    fun deletePaymentMethod(methodId: UUID, userId: Long): DeletePaymentMethodResponse {
        return transaction {
            val now = TimeUtils.currentUtcOffsetDateTime()
            val existingMethod =
                PaymentMethodsTable.selectAll()
                    .where { (PaymentMethodsTable.id eq methodId) and (PaymentMethodsTable.userId eq userId) }
                    .firstOrNull()
                    ?: throw Exception("Payment method not found for user")

            PaymentMethodsTable.deleteWhere { PaymentMethodsTable.id eq methodId }

            DeletePaymentMethodResponse(
                paymentMethodId = methodId.toString(),
                userId = userId,
                deletedAt = now.toString()
            )
        }
    }

    fun selectDefaultPaymentMethod(methodId: UUID, userId: Long): SelectWithdrawalMethodResponse {
        return transaction {
            val now = TimeUtils.currentUtcOffsetDateTime()

            PaymentMethodsTable
                .update({ (PaymentMethodsTable.userId eq userId) and (PaymentMethodsTable.isDefault eq true) }) {
                    it[PaymentMethodsTable.isDefault] = false
                    it[updatedAt] = now
                }

            PaymentMethodsTable
                .update({ (PaymentMethodsTable.id eq methodId) and (PaymentMethodsTable.userId eq userId) }) {
                    it[PaymentMethodsTable.isDefault] = true
                    it[updatedAt] = now
                }

            SelectWithdrawalMethodResponse(
                userId = userId,
                selectedMethodId = methodId.toString(),
                selectedAt = now.toString()
            )
        }
    }

    private fun mapToString(map: Map<String, String>): String {
        return map.entries.joinToString(",") { "${it.key}=${it.value}" }
    }
}