package xyz.robinjoon.growweek.common.contract.member

import xyz.robinjoon.growweek.common.domain.MemberId

interface MemberTokenPort {
    fun createToken(memberId: MemberId): String

    fun getExpirationInSeconds(): Long
}
