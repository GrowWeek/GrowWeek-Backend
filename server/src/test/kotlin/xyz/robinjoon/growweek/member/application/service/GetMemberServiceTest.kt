package xyz.robinjoon.growweek.member.application.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.member.domain.model.*
import xyz.robinjoon.growweek.member.domain.repository.MemberRepository
import java.time.LocalDateTime

class GetMemberServiceTest :
    BehaviorSpec({

        val memberRepository = mockk<MemberRepository>()
        val getMemberService = GetMemberService(memberRepository)

        Given("회원 조회 요청이 왔을 때") {

            When("존재하는 회원 ID로 조회하면") {
                val memberId = MemberId(1L)
                val now = LocalDateTime.now()
                val member =
                    Member.load(
                        id = memberId,
                        email = Email("test@example.com"),
                        password = Password("encodedPassword"),
                        nickname = Nickname("홍길동"),
                        status = MemberStatus.ACTIVE,
                        createdAt = now,
                        updatedAt = now,
                    )

                every { memberRepository.findAll(any()) } returns
                    OffsetPage(
                        items = listOf(member),
                        page = 0,
                        size = 1,
                        totalPage = 1,
                    )

                val result = getMemberService.getMember(memberId.value)

                Then("MemberDto가 반환된다") {
                    result shouldNotBe null
                    result!!.id shouldBe memberId
                    result.email shouldBe "test@example.com"
                    result.nickname shouldBe "홍길동"
                    result.status shouldBe "ACTIVE"
                }
            }

            When("존재하지 않는 회원 ID로 조회하면") {
                val memberId = 999L

                every { memberRepository.findAll(any()) } returns
                    OffsetPage(
                        items = emptyList(),
                        page = 0,
                        size = 1,
                        totalPage = 0,
                    )

                val result = getMemberService.getMember(memberId)

                Then("null이 반환된다") {
                    result shouldBe null
                }
            }
        }
    })
