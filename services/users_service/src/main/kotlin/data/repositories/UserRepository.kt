package com.example.data.repositories

import com.example.constants.ConflictException
import com.example.data.db.tables.UserTable
import com.example.hashing.SaltedHash
import io.ktor.server.plugins.*
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import users.UserRole
import users.UserStatus
import users.request.RegisterRequest
import users.response.UserResponse
import java.time.OffsetDateTime
import javax.naming.AuthenticationException

interface UserRepository {
    fun register(req: RegisterRequest, hashedPassword: SaltedHash): InsertStatement<Number>
    fun findById(id: Long): UserResponse?
    fun findByLogin(login: String): UserResponse?
    fun findByEmail(email: String): UserResponse?
    fun updateAvatar(userId: Long, avatarUrl: String): Int
    fun updateDesc(userId: Long, desc: String): Int
    fun updatePassword(userId: Long, hashedPassword: SaltedHash): Int
    fun getSaltedHash(login: String): SaltedHash
    fun updateStatus(userId: Long, status: UserStatus): Int
}

class UserRepositoryImpl: UserRepository {

    override fun register(req: RegisterRequest, hashedPassword: SaltedHash) = transaction {
        if (existsByLogin(req.login)) {
            throw ConflictException("Login '${req.login}' already exists")
        }
        try {
            UserTable.insert {
                it[login] = req.login
                it[email] = req.email
                it[hashPassword] = hashedPassword.hash
                it[UserTable.role] = UserRole.USER
                it[creationDate] = now()
                it[salt] = hashedPassword.salt
                it[avatar] = req.avatar ?: ""
                it[desc] = req.desc
                it[status] = UserStatus.ACTIVE
            }
        } catch (e: ExposedSQLException) {
          throw e
        }
    }

    override fun findById(id: Long): UserResponse? = transaction {
        UserTable
            .selectAll()
            .where { UserTable.id eq id }
            .firstOrNull()
            ?.let(::toUserResponse)
    }

    override fun findByLogin(login: String): UserResponse? = transaction {
        UserTable
            .selectAll()
            .where { UserTable.login eq login }
            .firstOrNull()
            ?.let(::toUserResponse)
    }

    override fun findByEmail(email: String): UserResponse? = transaction {
        UserTable
            .selectAll()
            .where { UserTable.email eq email }
            .firstOrNull()
            ?.let(::toUserResponse)
    }

    override fun updateAvatar(userId: Long, avatarUrl: String): Int = transaction {
        ensureUserExists(userId)
        try {
            val update = UserTable.update({ UserTable.id eq userId }) {
                it[avatar] = avatarUrl
            }
            update
        } catch (e: ExposedSQLException) {
            throw e
        }
    }

    override fun updateDesc(userId: Long, desc: String): Int = transaction {
        ensureUserExists(userId)
        try {
            val update = UserTable.update({ UserTable.id eq userId }) {
                it[UserTable.desc] = desc
            }
            update
        } catch (e: ExposedSQLException) {
            throw e
        }
    }

    override fun updatePassword(userId: Long, hashedPassword: SaltedHash): Int = transaction {
        ensureUserExists(userId)
        try {
            UserTable.update({ UserTable.id eq userId }) {
                it[UserTable.hashPassword] = hashedPassword.hash
                it[UserTable.salt] = hashedPassword.salt
            }
        } catch (e: ExposedSQLException) {
            throw e
        }
    }

    override fun updateStatus(userId: Long, status: UserStatus): Int = transaction {
        ensureUserExists(userId)
        try {
            UserTable.update({ UserTable.id eq userId }) {
               it[UserTable.status] = status
            }
        } catch (e: ExposedSQLException) {
            throw e
        }
    }

    override fun getSaltedHash(login: String): SaltedHash = transaction {
        val user = UserTable
            .select(UserTable.hashPassword, UserTable.salt)
            .where { UserTable.login eq login }
            .firstOrNull()
            ?: throw AuthenticationException("Invalid credentials") // Безопасная обработка отсутствия пользователя

        SaltedHash(
            hash = user[UserTable.hashPassword],
            salt = user[UserTable.salt]
        )
    }

    private fun existsByLogin(login: String): Boolean = transaction {
        UserTable
            .select(UserTable.id)
            .where { UserTable.login eq login }
            .count() > 0
    }

    private fun currentRole(userId: Long): UserRole? = transaction {
        UserTable
            .select(UserTable.role)
            .where { UserTable.id eq userId }
            .firstOrNull()
            ?.let { row ->
                row[UserTable.role]
            }
    }

    private fun ensureUserExists(userId: Long) {
        if (findById(userId) == null) {
            throw NotFoundException("User with id $userId not found")
        }
    }


    private fun toUserResponse(row: ResultRow) = UserResponse(
        id = row[UserTable.id],
        login = row[UserTable.login],
        role = currentRole(row[UserTable.id]) ?: UserRole.USER,
        creationDate = row[UserTable.creationDate].toString(),
        avatar = row[UserTable.avatar].takeUnless { it.isEmpty() },
        desc = row[UserTable.desc]
    )

    private fun now() = OffsetDateTime.now()
}