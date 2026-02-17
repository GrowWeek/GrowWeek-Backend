package xyz.robinjoon.growweek.common.port

import xyz.robinjoon.growweek.common.domain.MemberId

interface MemberTokenPort {
    fun createToken(memberId: MemberId): String

    fun getExpirationInSeconds(): Long
}
