package data.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import users.ReportReason
import java.time.OffsetDateTime

object ReportTable : Table("report_table") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(BanTable.id)
    val targetUser = long("target_user").references(UserTable.id, onDelete = ReferenceOption.CASCADE)
    val reason = enumerationByName<ReportReason>("reason", 32)
    val createdAt = timestampWithTimeZone("created_at").default(OffsetDateTime.now())
    val message = text("message").nullable()
    val issuer = long("issuer").references(UserTable.id, onDelete = ReferenceOption.CASCADE)
}