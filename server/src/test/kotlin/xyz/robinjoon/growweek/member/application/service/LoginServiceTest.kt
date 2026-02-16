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
import xyz.robinjoon.growweek.member.domain.repository.MemberRepository
import xyz.robinjoon.growweek.member.domain.service.AccessTokenProvider
import java.time.LocalDateTime

class LoginServiceTest :
    BehaviorSpec({

        val memberRepository = mockk<MemberRepository>()
        val passwordEncoder = mockk<PasswordEncoder>()
        val accessTokenProvider = mockk<AccessTokenProvider>()
        val loginService = LoginService(memberRepository, passwordEncoder, accessTokenProvider)

        Given("로그인 요청이 왔을 때") {
            val command =
                MemberApplicationCommand.Login(
                    email = "test@example.com",
                    password = "password123",
                )

            When("유효한 자격 증명으로 로그인을 시도하면") {
                val now = LocalDateTime.now()
                val activeMember =
                    Member.load(
                        id = MemberId(1L),
                        email = Email("test@example.com"),
                        password = Password("encodedPassword"),
                        nickname = Nickname("홍길동"),
                        status = MemberStatus.ACTIVE,
                        createdAt = now,
                        updatedAt = now,
                    )

                every { memberRepository.findAll(any()) } returns
                    OffsetPage(
                        items = listOf(activeMember),
                        page = 0,
                        size = 1,
                        totalPage = 1,
                    )
                every { passwordEncoder.matches(command.password, "encodedPassword") } returns true
                every { accessTokenProvider.createToken(MemberId(1L)) } returns "jwt.token.here"
                every { accessTokenProvider.getExpirationInSeconds() } returns 3600L

                val result = loginService.login(command)

                Then("토큰이 발급된다") {
                    result.accessToken shouldBe "jwt.token.here"
                    result.tokenType shouldBe "Bearer"
                    result.expiresIn shouldBe 3600L
                }

                Then("액세스 토큰이 생성된다") {
                    verify(exactly = 1) { accessTokenProvider.createToken(MemberId(1L)) }
                }
            }

            When("존재하지 않는 이메일로 로그인을 시도하면") {
                every { memberRepository.findAll(any()) } returns
                    OffsetPage(
                        items = emptyList(),
                        page = 0,
                        size = 1,
                        totalPage = 0,
                    )

                Then("IllegalArgumentException이 발생한다") {
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            loginService.login(command)
                        }
                    exception.message shouldBe "이메일 또는 비밀번호가 올바르지 않습니다"
                }
            }

            When("비활성화된 계정으로 로그인을 시도하면") {
                val now = LocalDateTime.now()
                val inactiveMember =
                    Member.load(
                        id = MemberId(1L),
                        email = Email("test@example.com"),
                        password = Password("encodedPassword"),
                        nickname = Nickname("홍길동"),
                        status = MemberStatus.INACTIVE,
                        createdAt = now,
                        updatedAt = now,
                    )

                every { memberRepository.findAll(any()) } returns
                    OffsetPage(
                        items = listOf(inactiveMember),
                        page = 0,
                        size = 1,
                        totalPage = 1,
                    )

                Then("IllegalStateException이 발생한다") {
                    val exception =
                        shouldThrow<IllegalStateException> {
                            loginService.login(command)
                        }
                    exception.message shouldBe "비활성화된 계정입니다"
                }
            }

            When("잘못된 비밀번호로 로그인을 시도하면") {
                val now = LocalDateTime.now()
                val activeMember =
                    Member.load(
                        id = MemberId(1L),
                        email = Email("test@example.com"),
                        password = Password("encodedPassword"),
                        nickname = Nickname("홍길동"),
                        status = MemberStatus.ACTIVE,
                        createdAt = now,
                        updatedAt = now,
                    )

                every { memberRepository.findAll(any()) } returns
                    OffsetPage(
                        items = listOf(activeMember),
                        page = 0,
                        size = 1,
                        totalPage = 1,
                    )
                every { passwordEncoder.matches(command.password, "encodedPassword") } returns false

                Then("IllegalArgumentException이 발생한다") {
                    val exception =
                        shouldThrow<IllegalArgumentException> {
                            loginService.login(command)
                        }
                    exception.message shouldBe "이메일 또는 비밀번호가 올바르지 않습니다"
                }
            }
        }
    })
