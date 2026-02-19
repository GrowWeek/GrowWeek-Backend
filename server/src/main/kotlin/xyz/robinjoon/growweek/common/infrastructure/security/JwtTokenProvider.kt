package xyz.robinjoon.growweek.common.infrastructure.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import xyz.robinjoon.growweek.common.contract.member.MemberTokenPort
import xyz.robinjoon.growweek.common.domain.MemberId
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expirationMs: Long,
) : MemberTokenPort {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    override fun createToken(memberId: MemberId): String {
        val now = Date()
        val expiration = Date(now.time + expirationMs)

        return Jwts
            .builder()
            .subject(memberId.value.toString())
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    fun getMemberIdFromToken(token: String): MemberId {
        val claims =
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload

        return MemberId(claims.subject.toLong())
    }

    fun validateToken(token: String): Boolean =
        try {
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }

    override fun getExpirationInSeconds(): Long = expirationMs / 1000
}
