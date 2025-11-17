package com.example.data.repositories

import com.example.data.db.tables.BanTable
import com.example.data.db.tables.FollowTable
import com.example.data.db.tables.UserTable
import com.example.hashing.HashingService
import com.example.hashing.SHA256HashingService
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import users.request.RegisterRequest
import java.util.*
import kotlin.test.*

private fun createBanTestDatabase(): Database {
    val db = Database.connect(
        url = "jdbc:h2:mem:ban_${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )

    transaction(db) {
        SchemaUtils.create(
            UserTable,
            BanTable
        )
    }

    return db
}

class BanRepositoryImplTest {

    private lateinit var database: Database
    private lateinit var userRepo: UserRepository
    private lateinit var banRepo: BanRepository
    private lateinit var hashingService: HashingService

    @BeforeTest
    fun setUp() {
        database = createBanTestDatabase()
        userRepo = UserRepositoryImpl()
        banRepo = BanRepositoryImpl(userRepo)
        hashingService = SHA256HashingService()

        userRepo.register(
            RegisterRequest("target", "t@mail.com", "test", "d", null),
            hashingService.generateSaltedHash("test")
        )
        userRepo.register(
            RegisterRequest("mod", "m@mail.com", "test", "d", null),
            hashingService.generateSaltedHash("test")
        )
    }

    @AfterTest
    fun tearDown() {
        transaction(database) {
            BanTable.deleteAll()
            UserTable.deleteAll()
        }
    }

    @Test
    fun `banUser should insert ban record`() {
        val target = userRepo.findByLogin("target")!!.id
        val mod = userRepo.findByLogin("mod")!!.id

        banRepo.banUser(target, mod, 5, "breaking rules")

        val stored = transaction(database) {
            BanTable.selectAll().where { BanTable.userId eq target }.single()
        }

        assertEquals(target, stored[BanTable.userId])
        assertEquals(mod, stored[BanTable.moderatorId])
        assertEquals(5, stored[BanTable.durationDays])
    }

    @Test
    fun `unbanUser should update unbannedAt`() {
        val target = userRepo.findByLogin("target")!!.id
        val mod = userRepo.findByLogin("mod")!!.id

        banRepo.banUser(target, mod, null, null)

        banRepo.unbanUser(target, mod)

        val stored = transaction(database) {
            BanTable.selectAll().where { BanTable.userId eq target }.single()
        }

        assertNotNull(stored[BanTable.unbannedAt])
    }
}
