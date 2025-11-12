package data.db.tables

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import wallet.PaymentType

object PaymentMethodsTable : UUIDTable("payment_methods") {
    val userId = long("user_id")
    val type = enumeration<PaymentType>("status").default(PaymentType.CARD)
    val details = text("details")
    val maskedDetails = varchar("masked_details", 255)
    val isDefault = bool("is_default").default(false)
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at").nullable()
}