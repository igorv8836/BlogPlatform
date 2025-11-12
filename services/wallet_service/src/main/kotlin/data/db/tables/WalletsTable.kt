package data.db.tables

import com.example.utils.TimeUtils
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

object WalletsTable : UUIDTable("wallets") {
    val userId = long("user_id")
    val balance = double("balance").default(0.0)
    val currency = varchar("currency", 3).default("RUB")
    val createdAt = timestampWithTimeZone("created_at")
        .default(TimeUtils.currentUtcOffsetDateTime())
    val updatedAt = timestampWithTimeZone("updated_at").nullable()
}