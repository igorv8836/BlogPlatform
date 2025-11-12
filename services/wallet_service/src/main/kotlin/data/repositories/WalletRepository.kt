package data.repositories

import com.example.utils.TimeUtils
import data.db.tables.WalletsTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import wallet.response.CreatedWalletResponse
import wallet.response.WalletBalanceResponse

class WalletRepository {
    fun getUserBalance(userId: Long): WalletBalanceResponse {
        return transaction {
            val balance: Double = WalletsTable
                .selectAll().where { WalletsTable.userId eq userId }
                .firstOrNull()?.get(WalletsTable.balance) ?: 0.0

            WalletBalanceResponse(currentBalance = balance)
        }
    }

    fun updateBalance(userId: Long, newBalance: Double): Double {
        return transaction {
            val now = TimeUtils.currentUtcOffsetDateTime()
            WalletsTable.update({ WalletsTable.userId eq userId }) {
                it[WalletsTable.balance] = newBalance
                it[WalletsTable.updatedAt] = now
            }
            newBalance
        }
    }

    fun createWallet(userId: Long): CreatedWalletResponse {
        return transaction {
            val now = TimeUtils.currentUtcOffsetDateTime()
            WalletsTable.insert {
                it[WalletsTable.userId] = userId
                it[WalletsTable.createdAt] = now
            }
            CreatedWalletResponse(
                userId = userId,
                balance = 0.0,
                currency = "RUB",
                createdAt = now.toString()
            )
        }
    }

    fun credit(userId: Long, amount: Double): WalletBalanceResponse {
        return transaction {
            val currentBalance = getUserBalance(userId).currentBalance
            val newBalance = currentBalance + amount
            updateBalance(userId, newBalance)
            WalletBalanceResponse(newBalance)
        }
    }

    fun debit(userId: Long, amount: Double): WalletBalanceResponse {
        return transaction {
            val currentBalance = getUserBalance(userId).currentBalance
            if (currentBalance >= amount) {
                val newBalance = currentBalance - amount
                updateBalance(userId, newBalance)
                WalletBalanceResponse(newBalance)
            } else {
                throw Exception("Not enough money in the balance")
            }
        }
    }
}