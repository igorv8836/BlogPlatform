package com.example.data.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import users.UserRole
import users.UserStatus
import java.time.OffsetDateTime

object UserTable : Table("user_table") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val login = varchar("login", 50)
    val email = varchar("email", 255)
    val hashPassword = varchar(name = "password_hash", length = 64)
    val role = enumerationByName<UserRole>("role", 32).default(UserRole.USER)
    val creationDate = timestampWithTimeZone("created_at").default(OffsetDateTime.now())
    val salt = varchar(name = "salt", length = 64)
    val avatar = varchar(name="image", length = 255).default("")
    val desc = text("desc").default("")
    val status = enumerationByName<UserStatus>("status", 32).default(UserStatus.ACTIVE)
}