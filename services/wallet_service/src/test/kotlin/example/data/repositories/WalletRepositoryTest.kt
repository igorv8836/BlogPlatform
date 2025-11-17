package com.example.data.repositories

import data.db.tables.WalletsTable
import data.repositories.WalletRepository
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*
import kotlin.test.*

private fun createTestDatabase(): Database {
    val db = Database.connect(
        url = "jdbc:h2:mem:wallet_${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )
    transaction(db) {
        SchemaUtils.create(
            WalletsTable
        )
    }
    return db
}

class WalletRepositoryTest {

    private lateinit var database: Database
    private lateinit var walletRepository: WalletRepository

    @BeforeTest
    fun setUp() {
        database = createTestDatabase()
        walletRepository = WalletRepository()
    }

    @AfterTest
    fun tearDown() {
        transaction(database) {
            WalletsTable.deleteAll()
        }
    }

    @Test
    fun `createWallet should insert a new wallet record and return CreatedWalletResponse`() {
        val userId = 100L
        val response = walletRepository.createWallet(userId)

        assertNotNull(response)
        assertEquals(userId, response.userId)
        assertEquals(0.0, response.balance)

        val stored = transaction(database) {
            WalletsTable.selectAll().where { WalletsTable.userId eq userId }.singleOrNull()
        }
        assertNotNull(stored)
        assertEquals(userId, stored[WalletsTable.userId])
        assertEquals(0.0, stored[WalletsTable.balance])
    }

    @Test
    fun `getUserBalance should return correct balance`() {
        val userId = 100L
        val initialBalance = 100.0
        transaction(database) {
            WalletsTable.insert {
                it[WalletsTable.userId] = userId
                it[balance] = initialBalance
            }
        }

        val response = walletRepository.getUserBalance(userId)

        assertNotNull(response)
        assertEquals(initialBalance, response.currentBalance)
    }

    @Test
    fun `debit should reduce balance and return updated WalletBalanceResponse`() {
        val userId = 100L
        val initialBalance = 100.0
        val debitAmount = 30.0
        val expectedBalance = initialBalance - debitAmount

        transaction(database) {
            WalletsTable.insert {
                it[WalletsTable.userId] = userId
                it[balance] = initialBalance
            }
        }

        val response = walletRepository.debit(userId, debitAmount)

        assertNotNull(response)
        assertEquals(expectedBalance, response.currentBalance)

        val storedBalance = transaction(database) {
            WalletsTable.selectAll().where { WalletsTable.userId eq userId }.single()[WalletsTable.balance]
        }
        assertEquals(expectedBalance, storedBalance)
    }

    @Test
    fun `debit should fail if insufficient funds`() {
        val userId = 100L
        val initialBalance = 100.0
        val debitAmount = 150.0

        transaction(database) {
            WalletsTable.insert {
                it[WalletsTable.userId] = userId
                it[balance] = initialBalance
            }
        }

        assertFailsWith<Exception> {
            walletRepository.debit(userId, debitAmount)
        }

        val storedBalance = transaction(database) {
            WalletsTable.selectAll().where { WalletsTable.userId eq userId }.single()[WalletsTable.balance]
        }
        assertEquals(initialBalance, storedBalance)
    }

    @Test
    fun `credit should increase balance and return updated WalletBalanceResponse`() {
        val userId = 100L
        val initialBalance = 100.0
        val creditAmount = 50.0
        val expectedBalance = initialBalance + creditAmount

        transaction(database) {
            WalletsTable.insert {
                it[WalletsTable.userId] = userId
                it[balance] = initialBalance
            }
        }

        val response = walletRepository.credit(userId, creditAmount)

        assertNotNull(response)
        assertEquals(expectedBalance, response.currentBalance)

        val storedBalance = transaction(database) {
            WalletsTable.selectAll().where { WalletsTable.userId eq userId }.single()[WalletsTable.balance]
        }
        assertEquals(expectedBalance, storedBalance)
    }
}