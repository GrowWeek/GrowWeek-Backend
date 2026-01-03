package xyz.robinjoon.growweek.member.domain.model

import xyz.robinjoon.growweek.common.domain.MemberId

data class Member(
    val id: MemberId,
    val email: Email,
    val password: EncodedPassword,
    val name: MemberName
)
