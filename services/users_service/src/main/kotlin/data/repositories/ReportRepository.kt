package data.repositories

import com.example.constants.ConflictException
import data.db.tables.BanTable
import data.db.tables.BanTable.unbannedBy
import data.db.tables.ReportTable
import data.db.tables.UserTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import users.ReportReason
import java.time.OffsetDateTime

interface ReportRepository {
    fun create(
        targetUserId: Long,
        issuerUserId: Long,
        reason: ReportReason,
        message: String?
    ): InsertStatement<Number>
}
class ReportRepositoryImpl(
    private val repo: UserRepository
): ReportRepository {

    override fun create(
        targetUserId: Long,
        issuerUserId: Long,
        reason: ReportReason,
        message: String?
    ) = transaction {

        if (repo.findById(targetUserId) == null) throw NotFoundException("Target user not found")
        if (repo.findById(issuerUserId) == null) throw NotFoundException("Moderator not found")

        ReportTable.insert {
            it[this.targetUser] = targetUserId
            it[this.issuer] = issuerUserId
            it[this.reason] = reason
            it[this.message] = message
            it[createdAt] = now()
        }


    }

    private fun now() = OffsetDateTime.now()
}