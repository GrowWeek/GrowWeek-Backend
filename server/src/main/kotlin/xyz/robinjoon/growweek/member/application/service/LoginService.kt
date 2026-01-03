package xyz.robinjoon.growweek.member.application.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.common.infrastructure.security.JwtTokenProvider
import xyz.robinjoon.growweek.member.application.command.MemberApplicationCommand
import xyz.robinjoon.growweek.member.application.dto.TokenDto
import xyz.robinjoon.growweek.member.application.usecase.LoginUseCase
import xyz.robinjoon.growweek.member.domain.model.Email
import xyz.robinjoon.growweek.member.domain.model.query.MemberQuery
import xyz.robinjoon.growweek.member.domain.repository.MemberRepository

@Service
class LoginService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
) : LoginUseCase {
    @Transactional(readOnly = true)
    override fun login(command: MemberApplicationCommand.Login): TokenDto {
        val email = Email(command.email)

        val member =
            memberRepository.findAll(MemberQuery.byEmail(email)).items.firstOrNull()
                ?: throw IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다")

        if (!member.isActive()) {
            throw IllegalStateException("비활성화된 계정입니다")
        }

        if (!passwordEncoder.matches(command.password, member.password.value)) {
            throw IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다")
        }

        val accessToken = jwtTokenProvider.createToken(member.id)

        return TokenDto(
            accessToken = accessToken,
            expiresIn = jwtTokenProvider.getExpirationInSeconds(),
        )
    }
}
