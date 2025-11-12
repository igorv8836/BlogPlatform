package data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import wallet.SubscriptionStatus

object SubscriptionsTable : UUIDTable("subscriptions") {
    val userId = long("user_id")
    val authorId = long("author_id")
    val amount = double("amount")
    val currency = varchar("currency", 3).default("RUB")
    val paymentMethodId = uuid("payment_method_id").references(PaymentMethodsTable.id)
    val status = enumeration<SubscriptionStatus>("status").default(SubscriptionStatus.ACTIVE)
    val startedAt = timestampWithTimeZone("started_at")
    val cancelledAt = timestampWithTimeZone("cancelled_at").nullable()
    val nextPaymentAt = timestampWithTimeZone("next_payment_at").nullable()
}