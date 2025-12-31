package xyz.robinjoon.growweek.member.infrastructure.persistence

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.datetime
import xyz.robinjoon.growweek.member.domain.model.MemberStatus

object MemberTable : LongIdTable("members") {
    val email = varchar("email", 100).uniqueIndex()
    val password = varchar("password", 200)
    val nickname = varchar("nickname", 50)
    val status = varchar("status", 20).default(MemberStatus.ACTIVE.name)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        index(false, status)
    }
}
