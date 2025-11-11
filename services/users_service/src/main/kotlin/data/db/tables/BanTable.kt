package data.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import users.ReportReason
import java.time.OffsetDateTime

object BanTable : Table("ban_table") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(BanTable.id)
    val userId = long("user_id").references(UserTable.id, onDelete = ReferenceOption.CASCADE)
    val moderatorId = long("moderator_id").references(UserTable.id)
    val reason = enumerationByName<ReportReason>("reason", 32)
    val createdAt = timestampWithTimeZone("created_at").default(OffsetDateTime.now())
    val durationDays = integer("duration_days").nullable()
    val expiresAt = timestampWithTimeZone("expires_at").nullable()
    val message = text("message").nullable()
    val unbannedBy = long("unbanned_by").references(UserTable.id).nullable()
    val unbannedAt = timestampWithTimeZone("unbanned_at").nullable()
}