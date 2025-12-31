package xyz.robinjoon.growweek.member.application.dto

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.member.domain.model.Member
import java.time.LocalDateTime

data class MemberDto(
    val id: MemberId,
    val email: String,
    val nickname: String,
    val status: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(member: Member): MemberDto {
            return MemberDto(
                id = member.id,
                email = member.email.value,
                nickname = member.nickname.value,
                status = member.status.name,
                createdAt = member.createdAt
            )
        }
    }
}
