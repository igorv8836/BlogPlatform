package com.example.data.repositories

import com.example.data.db.tables.BanTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import users.UserStatus
import java.time.OffsetDateTime

interface BanRepository {
    fun banUser(
        targetUserId: Long,
        moderatorId: Long,
        durationDays: Int?,
        message: String?
    ): InsertStatement<Number>

    fun unbanUser(
        targetUserId: Long,
        moderatorId: Long,
    ): Int
}

class BanRepositoryImpl(
    private val repo: UserRepository
): BanRepository {

    override fun banUser(
        targetUserId: Long,
        moderatorId: Long,
        durationDays: Int?,
        message: String?
    ) = transaction {

        if (repo.findById(targetUserId) == null) throw NotFoundException("Target user not found")
        if (repo.findById(moderatorId) == null) throw NotFoundException("Moderator not found")


        val expiresAt = durationDays?.let {
            OffsetDateTime.now().plusDays(it.toLong())
        }
        repo.updateStatus(targetUserId, UserStatus.BANNED)

        BanTable.insert {
            it[userId] = targetUserId
            it[this.moderatorId] = moderatorId
            it[this.durationDays] = durationDays
            it[this.expiresAt] = expiresAt
            it[this.message] = message
            it[createdAt] = now()
        }
    }

    override fun unbanUser(
        targetUserId: Long,
        moderatorId: Long,
    ) = transaction {
        val activeBan = BanTable
            .selectAll()
            .where {
                (BanTable.userId eq targetUserId) and
                        (BanTable.unbannedAt.isNull()) and
                        (BanTable.expiresAt greaterEq now() or BanTable.expiresAt.isNull())
            }
            .firstOrNull() ?: throw NotFoundException("No active ban found")

        BanTable.update({ BanTable.id eq activeBan[BanTable.id] }) {
            it[unbannedAt] = now()
            it[unbannedBy] = moderatorId
        }

        repo.updateStatus(targetUserId, UserStatus.SUSPENDED)
    }

    private fun now() = OffsetDateTime.now()
}