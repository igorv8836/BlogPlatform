package com.example.data.repositories

import com.example.data.db.tables.FollowTable
import com.example.data.db.tables.UserTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import users.response.FollowersResponse
import users.response.UserResponse

interface FollowRepository {
    fun follow(followerId: Long, followeeId: Long): Boolean
    fun unfollow(followerId: Long, followeeId: Long): Boolean
    fun isFollowing(followerId: Long, followeeId: Long): Boolean
    fun getFollowers(userId: Long): FollowersResponse
    fun getFollowing(userId: Long): List<UserResponse>
}

class FollowRepositoryImpl(
    private val repo: UserRepository
): FollowRepository {
    override fun follow(followerId: Long, followeeId: Long): Boolean = transaction {
        if (followerId == followeeId) {
            throw BadRequestException("Cannot follow yourself")
        }
        if (repo.findById(followeeId) == null) {
            throw NotFoundException("User to follow not found")
        }
        if (isFollowing(followerId, followeeId)) {
            return@transaction false
        }
        FollowTable.insert {
            it[this.followerId] = followerId
            it[this.followeeId] = followeeId
        }
        true
    }

    override fun unfollow(followerId: Long, followeeId: Long): Boolean = transaction {
        val deleted = FollowTable.deleteWhere {
            (FollowTable.followerId eq followerId) and
                    (FollowTable.followeeId eq followeeId)
        }
        deleted > 0
    }

    override fun isFollowing(followerId: Long, followeeId: Long): Boolean = transaction {
        FollowTable
            .select(FollowTable.id)
            .where {
                (FollowTable.followerId eq followerId) and
                        (FollowTable.followeeId eq followeeId)
            }
            .empty().not()
    }

    override fun getFollowers(userId: Long): FollowersResponse = transaction {
        val followersId = FollowTable
            .innerJoin(UserTable, { FollowTable.followerId }, { UserTable.id })
            .select(UserTable.columns)
            .where { FollowTable.followeeId eq userId }
            .orderBy(FollowTable.createdAt, SortOrder.DESC)
            .map { row ->
                row[UserTable.id]
            }
        FollowersResponse(followersId)
    }

    override fun getFollowing(userId: Long): List<UserResponse> = transaction {
        FollowTable
            .innerJoin(UserTable, { FollowTable.followerId }, { UserTable.id })
            .select(UserTable.columns)
            .where { FollowTable.followerId eq userId }
            .orderBy(FollowTable.createdAt, SortOrder.DESC)
            .map { row ->
                UserResponse(
                    id = row[UserTable.id],
                    login = row[UserTable.login],
                    creationDate = row[UserTable.creationDate].toString(),
                    role = row[UserTable.role],
                    desc = row[UserTable.desc],
                    avatar = row[UserTable.avatar],
                )
            }
    }
}