package xyz.robinjoon.growweek.member.infrastructure.external

import org.springframework.stereotype.Component
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.infrastructure.security.JwtTokenProvider
import xyz.robinjoon.growweek.member.domain.service.AccessTokenProvider

@Component
class JwtAccessTokenProvider(
    private val jwtTokenProvider: JwtTokenProvider,
) : AccessTokenProvider {
    override fun createToken(memberId: MemberId): String = jwtTokenProvider.createToken(memberId)

    override fun getExpirationInSeconds(): Long = jwtTokenProvider.getExpirationInSeconds()
}
