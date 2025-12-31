package xyz.robinjoon.growweek.member.application.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.member.application.command.MemberApplicationCommand
import xyz.robinjoon.growweek.member.application.dto.MemberDto
import xyz.robinjoon.growweek.member.application.usecase.SignUpUseCase
import xyz.robinjoon.growweek.member.domain.model.Email
import xyz.robinjoon.growweek.member.domain.model.Nickname
import xyz.robinjoon.growweek.member.domain.model.Password
import xyz.robinjoon.growweek.member.domain.model.command.MemberCommand
import xyz.robinjoon.growweek.member.domain.model.query.MemberQuery
import xyz.robinjoon.growweek.member.domain.repository.MemberRepository

@Service
class SignUpService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder
) : SignUpUseCase {

    @Transactional
    override fun signUp(command: MemberApplicationCommand.SignUp): MemberDto {
        val email = Email(command.email)

        // 이메일 중복 검사
        val existingMember = memberRepository.findAll(MemberQuery.byEmail(email)).items.firstOrNull()
        if (existingMember != null) {
            throw IllegalArgumentException("이미 사용 중인 이메일입니다: ${command.email}")
        }

        // 비밀번호 유효성 검사
        Password.validate(command.password)

        // 비밀번호 암호화
        val encodedPassword = passwordEncoder.encode(command.password)!!

        // 회원 생성
        val createCommand = MemberCommand.CreateMember(
            email = email,
            password = Password(encodedPassword),
            nickname = Nickname(command.nickname)
        )

        val savedMembers = memberRepository.saveAll(listOf(createCommand))
        val member = savedMembers.first()

        return MemberDto.from(member)
    }
}
