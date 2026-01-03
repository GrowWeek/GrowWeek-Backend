package xyz.robinjoon.growweek.member.infrastructure

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import xyz.robinjoon.growweek.member.domain.model.EncodedPassword
import xyz.robinjoon.growweek.member.domain.model.RawPassword
import xyz.robinjoon.growweek.member.domain.service.PasswordEncoder

@Component
class BcryptPasswordEncoder(
    private val bcryptPasswordEncoder: BCryptPasswordEncoder = BCryptPasswordEncoder()
) : PasswordEncoder {

    override fun encode(rawPassword: RawPassword): EncodedPassword {
        val encoded = bcryptPasswordEncoder.encode(rawPassword.value)
        return EncodedPassword(encoded)
    }

    override fun matches(rawPassword: RawPassword, encodedPassword: EncodedPassword): Boolean {
        return bcryptPasswordEncoder.matches(rawPassword.value, encodedPassword.value)
    }
}
