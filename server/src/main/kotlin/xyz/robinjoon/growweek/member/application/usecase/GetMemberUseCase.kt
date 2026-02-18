package xyz.robinjoon.growweek.member.application.usecase

import xyz.robinjoon.growweek.member.application.dto.MemberDto

interface GetMemberUseCase {
    fun getMember(memberId: Long): MemberDto?
}
