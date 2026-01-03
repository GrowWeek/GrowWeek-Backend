package xyz.robinjoon.growweek.member.domain.model

import xyz.robinjoon.growweek.common.domain.MemberId
import java.time.LocalDateTime

data class Member(
    val id: MemberId,
    val email: Email,
    val password: Password,
    val nickname: Nickname,
    val status: MemberStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun create(
            email: Email,
            password: Password,
            nickname: Nickname,
        ): Member {
            val now = LocalDateTime.now()
            return Member(
                id = MemberId(0),
                email = email,
                password = password,
                nickname = nickname,
                status = MemberStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun load(
            id: MemberId,
            email: Email,
            password: Password,
            nickname: Nickname,
            status: MemberStatus,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
        ): Member =
            Member(
                id = id,
                email = email,
                password = password,
                nickname = nickname,
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
    }

    fun isActive(): Boolean = status == MemberStatus.ACTIVE

    fun deactivate(): Member =
        copy(
            status = MemberStatus.INACTIVE,
            updatedAt = LocalDateTime.now(),
        )

    fun updateNickname(newNickname: Nickname): Member =
        copy(
            nickname = newNickname,
            updatedAt = LocalDateTime.now(),
        )

    fun withId(newId: MemberId): Member = copy(id = newId)
}
