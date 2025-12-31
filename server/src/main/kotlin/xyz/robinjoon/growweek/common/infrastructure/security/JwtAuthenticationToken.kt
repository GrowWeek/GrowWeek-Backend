package xyz.robinjoon.growweek.common.infrastructure.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import xyz.robinjoon.growweek.common.domain.MemberId

class JwtAuthenticationToken(
    val memberId: MemberId
) : AbstractAuthenticationToken(emptyList()) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null

    override fun getPrincipal(): MemberId = memberId
}
