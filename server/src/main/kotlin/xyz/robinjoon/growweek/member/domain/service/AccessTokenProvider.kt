package xyz.robinjoon.growweek.member.domain.service

import xyz.robinjoon.growweek.common.domain.MemberId

interface AccessTokenProvider {
    fun createToken(memberId: MemberId): String

    fun getExpirationInSeconds(): Long
}
