package xyz.robinjoon.growweek.member.presentation.rest.response

import xyz.robinjoon.growweek.member.application.dto.MemberDto
import java.time.format.DateTimeFormatter

data class MemberResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val createdAt: String,
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_DATE_TIME

        fun from(dto: MemberDto): MemberResponse =
            MemberResponse(
                id = dto.id.value,
                email = dto.email,
                nickname = dto.nickname,
                createdAt = dto.createdAt.format(formatter),
            )
    }
}
