package com.example.data.repositories

import com.example.data.db.tables.FollowTable
import com.example.data.db.tables.UserTable
import com.example.hashing.HashingService
import com.example.hashing.SHA256HashingService
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import users.request.RegisterRequest
import java.util.*
import kotlin.test.*

private fun createFollowTestDatabase(): Database {
    val db = Database.connect(
        url = "jdbc:h2:mem:follow_${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )

    transaction(db) {
        SchemaUtils.create(
            UserTable,
            FollowTable
        )
    }

    return db
}

class FollowRepositoryImplTest {

    private lateinit var database: Database
    private lateinit var userRepo: UserRepository
    private lateinit var repo: FollowRepository
    private lateinit var hashingService: HashingService

    @BeforeTest
    fun setUp() {
        database = createFollowTestDatabase()
        userRepo = UserRepositoryImpl()
        repo = FollowRepositoryImpl(userRepo)
        hashingService = SHA256HashingService()

        // Создаем 2 пользователя
        userRepo.register(
            RegisterRequest("u1", "u1@mail.com", "test", "d", null),
            hashingService.generateSaltedHash("test")
        )
        userRepo.register(
            RegisterRequest("u2", "u2@mail.com", "test", "d", null),
            hashingService.generateSaltedHash("test")
        )
    }

    @AfterTest
    fun tearDown() {
        transaction(database) {
            FollowTable.deleteAll()
            UserTable.deleteAll()
        }
    }

    @Test
    fun `follow should create a record`() {
        val user1 = userRepo.findByLogin("u1")!!.id
        val user2 = userRepo.findByLogin("u2")!!.id

        val result = repo.follow(user1, user2)
        assertTrue(result)

        val exists = repo.isFollowing(user1, user2)
        assertTrue(exists)
    }

    @Test
    fun `follow should not allow duplicates`() {
        val id1 = userRepo.findByLogin("u1")!!.id
        val id2 = userRepo.findByLogin("u2")!!.id

        repo.follow(id1, id2)
        val again = repo.follow(id1, id2)

        assertFalse(again)
    }

    @Test
    fun `unfollow should remove record`() {
        val a = userRepo.findByLogin("u1")!!.id
        val b = userRepo.findByLogin("u2")!!.id

        repo.follow(a, b)
        val result = repo.unfollow(a, b)

        assertTrue(result)
        assertFalse(repo.isFollowing(a, b))
    }
}