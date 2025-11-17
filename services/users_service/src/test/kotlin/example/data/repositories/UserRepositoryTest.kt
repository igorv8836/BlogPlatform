package com.example.data.repositories

import com.example.constants.ConflictException
import com.example.data.db.tables.BanTable
import com.example.data.db.tables.FollowTable
import com.example.data.db.tables.UserTable
import com.example.hashing.HashingService
import com.example.hashing.SHA256HashingService
import com.example.hashing.SaltedHash
import io.ktor.server.plugins.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import posts.request.CreatePostRequest
import posts.request.UpdatePostRequest
import users.request.RegisterRequest
import java.util.*
import kotlin.test.*

private fun createUserTestDatabase(): Database {
    val db = Database.connect(
        url = "jdbc:h2:mem:users_${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    )
    transaction(db) {
        SchemaUtils.create(
            UserTable
        )
    }
    return db
}

class UserRepositoryImplTest {

    private lateinit var database: Database
    private lateinit var repo: UserRepository
    private lateinit var hashingService: HashingService

    @BeforeTest
    fun setUp() {
        database = createUserTestDatabase()
        repo = UserRepositoryImpl()
        hashingService = SHA256HashingService()
    }

    @AfterTest
    fun tearDown() {
        transaction(database) {
            UserTable.deleteAll()
        }
    }

    @Test
    fun `register should insert new user`() {
        val req = RegisterRequest(
            login = "testUser",
            email = "user@mail.com",
            password = "test",
            avatar = null,
            desc = "Hello"
        )
        val hashedPassword = hashingService.generateSaltedHash(req.password)

        repo.register(req, hashedPassword)

        val stored = transaction(database) {
            UserTable.selectAll().where { UserTable.login eq "testUser" }.single()
        }

        assertEquals("testUser", stored[UserTable.login])
        assertEquals("user@mail.com", stored[UserTable.email])
        assertEquals(hashedPassword.hash, stored[UserTable.hashPassword])
        assertEquals(hashedPassword.salt, stored[UserTable.salt])
    }

    @Test
    fun `register should fail if login exists`() {
        val req = RegisterRequest(
            "test", "mail@mail.com", "test", "desc", null)
        val hashedPassword = hashingService.generateSaltedHash(req.password)

        repo.register(req, hashedPassword)

        assertFailsWith<ConflictException> {
            repo.register(req, hashedPassword)
        }
    }

    @Test
    fun `findByLogin should return user`() {
        val req = RegisterRequest("testLogin", "m@mail.com", "test", "desc", null)
        val hashedPassword = hashingService.generateSaltedHash(req.password)
        repo.register(req, hashedPassword)

        val user = repo.findByLogin("testLogin")
        assertNotNull(user)
        assertEquals("testLogin", user.login)
    }

    @Test
    fun `updateAvatar should change avatar`() {
        val req = RegisterRequest("user", "u@mail.com", "test", "desc", null)
        val hashedPassword = hashingService.generateSaltedHash(req.password)

        val inserted = repo.register(req, hashedPassword)

        val id = repo.findByLogin("user")!!.id
        repo.updateAvatar(id, "http://new-avatar")

        val stored = repo.findById(id)
        assertEquals("http://new-avatar", stored!!.avatar)
    }

    @Test
    fun `updatePassword should modify password and salt`() {
        val req = RegisterRequest("user", "email@mail.com", "test", "desc", null)
        val hashedPassword = hashingService.generateSaltedHash(req.password)
        repo.register(req, hashedPassword)

        val id = repo.findByLogin("user")!!.id

        val newHashedPassword = hashingService.generateSaltedHash("newTest")

        repo.updatePassword(id, newHashedPassword)

        val stored = transaction(database) {
            UserTable.selectAll().where { UserTable.id eq id }.single()
        }

        assertEquals(newHashedPassword.hash, stored[UserTable.hashPassword])
        assertEquals(newHashedPassword.salt, stored[UserTable.salt])
    }

    @Test
    fun `getSaltedHash should return correct hash`() {
        val req = RegisterRequest("user", "email@mail.com", "test", "desc", null)
        val hashedPassword = hashingService.generateSaltedHash(req.password)
        repo.register(req, hashedPassword)

        val salted = repo.getSaltedHash("user")

        assertEquals(hashedPassword.hash, salted.hash)
        assertEquals(hashedPassword.salt, salted.salt)
    }
}
