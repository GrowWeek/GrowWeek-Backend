package xyz.robinjoon.growweek.member.application.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.crypto.password.PasswordEncoder
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.member.application.command.MemberApplicationCommand
import xyz.robinjoon.growweek.member.domain.model.*
import xyz.robinjoon.growweek.member.domain.model.command.MemberCommand
import xyz.robinjoon.growweek.member.domain.repository.MemberRepository
import java.time.LocalDateTime

class SignUpServiceTest : BehaviorSpec({

    val memberRepository = mockk<MemberRepository>()
    val passwordEncoder = mockk<PasswordEncoder>()
    val signUpService = SignUpService(memberRepository, passwordEncoder)

    Given("회원가입 요청이 왔을 때") {
        val command = MemberApplicationCommand.SignUp(
            email = "test@example.com",
            password = "password123",
            nickname = "홍길동"
        )

        When("유효한 정보로 회원가입을 시도하면") {
            val encodedPassword = "\$2a\$10\$encodedPassword"
            val now = LocalDateTime.now()
            val savedMember = Member.load(
                id = MemberId(1L),
                email = Email("test@example.com"),
                password = Password(encodedPassword),
                nickname = Nickname("홍길동"),
                status = MemberStatus.ACTIVE,
                createdAt = now,
                updatedAt = now
            )

            every { memberRepository.findAll(any()) } returns OffsetPage(
                items = emptyList(),
                page = 0,
                size = 1,
                totalPage = 0
            )
            every { passwordEncoder.encode(any()) } returns encodedPassword
            every { memberRepository.saveAll(any<List<MemberCommand>>()) } returns listOf(savedMember)

            val result = signUpService.signUp(command)

            Then("회원이 생성되고 MemberDto가 반환된다") {
                result.email shouldBe "test@example.com"
                result.nickname shouldBe "홍길동"
                result.status shouldBe "ACTIVE"
            }

            Then("비밀번호가 암호화된다") {
                verify(exactly = 1) { passwordEncoder.encode(command.password) }
            }

            Then("Repository에 저장된다") {
                verify(exactly = 1) { memberRepository.saveAll(any<List<MemberCommand>>()) }
            }
        }

        When("이미 존재하는 이메일로 회원가입을 시도하면") {
            val existingMember = Member.load(
                id = MemberId(1L),
                email = Email("test@example.com"),
                password = Password("encodedPassword"),
                nickname = Nickname("기존회원"),
                status = MemberStatus.ACTIVE,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            every { memberRepository.findAll(any()) } returns OffsetPage(
                items = listOf(existingMember),
                page = 0,
                size = 1,
                totalPage = 1
            )

            Then("IllegalArgumentException이 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    signUpService.signUp(command)
                }
                exception.message shouldBe "이미 사용 중인 이메일입니다: test@example.com"
            }
        }

        When("8자 미만의 비밀번호로 회원가입을 시도하면") {
            val shortPasswordCommand = MemberApplicationCommand.SignUp(
                email = "new@example.com",
                password = "1234567",
                nickname = "홍길동"
            )

            every { memberRepository.findAll(any()) } returns OffsetPage(
                items = emptyList(),
                page = 0,
                size = 1,
                totalPage = 0
            )

            Then("IllegalArgumentException이 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    signUpService.signUp(shortPasswordCommand)
                }
                exception.message shouldBe "비밀번호는 8자 이상이어야 합니다"
            }
        }
    }
})
