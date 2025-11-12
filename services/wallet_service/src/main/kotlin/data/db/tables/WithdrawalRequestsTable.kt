package data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import wallet.Currency
import wallet.WithdrawalStatus

object WithdrawalRequestsTable : UUIDTable("withdrawal_requests") {
    val paymentMethodId = uuid("payment_method_id").references(PaymentMethodsTable.id)
    val userId = long("user_id")
    val amount = double("amount")
    val currency = enumeration<Currency>("currency").default(Currency.RUB)
    val status = text("status").default(WithdrawalStatus.PENDING.name)
    val requestedAt = timestampWithTimeZone("requested_at")
    val processedAt = timestampWithTimeZone("processed_at").nullable()
}