package com.example.data.db.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import java.time.OffsetDateTime

object FollowTable : Table("user_follows") {
    val id = long("id").autoIncrement()
    override val primaryKey = PrimaryKey(FollowTable.id)
    val followerId = long("follower_id").references(UserTable.id, onDelete = ReferenceOption.CASCADE)
    val followeeId = long("followee_id").references(UserTable.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = timestampWithTimeZone("created_at").default(OffsetDateTime.now())
}