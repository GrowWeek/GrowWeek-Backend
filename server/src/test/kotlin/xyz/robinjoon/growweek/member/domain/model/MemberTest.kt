package xyz.robinjoon.growweek.member.domain.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import xyz.robinjoon.growweek.common.domain.MemberId

class MemberTest : BehaviorSpec({

    Given("Member 엔티티 생성 시") {

        When("create 팩토리 메서드를 사용하면") {
            val email = Email("test@example.com")
            val password = Password("encodedPassword123")
            val nickname = Nickname("홍길동")

            val member = Member.create(email, password, nickname)

            Then("기본 ID는 0이다") {
                member.id.value shouldBe 0L
            }

            Then("상태는 ACTIVE이다") {
                member.status shouldBe MemberStatus.ACTIVE
            }

            Then("isActive()는 true를 반환한다") {
                member.isActive() shouldBe true
            }

            Then("생성 시간과 수정 시간이 설정된다") {
                member.createdAt shouldNotBe null
                member.updatedAt shouldNotBe null
            }
        }

        When("load 팩토리 메서드를 사용하면") {
            val id = MemberId(1L)
            val email = Email("test@example.com")
            val password = Password("encodedPassword123")
            val nickname = Nickname("홍길동")
            val status = MemberStatus.INACTIVE
            val createdAt = java.time.LocalDateTime.of(2024, 1, 1, 0, 0)
            val updatedAt = java.time.LocalDateTime.of(2024, 1, 2, 0, 0)

            val member = Member.load(id, email, password, nickname, status, createdAt, updatedAt)

            Then("모든 값이 그대로 로드된다") {
                member.id shouldBe id
                member.email shouldBe email
                member.password shouldBe password
                member.nickname shouldBe nickname
                member.status shouldBe status
                member.createdAt shouldBe createdAt
                member.updatedAt shouldBe updatedAt
            }
        }
    }

    Given("활성화된 Member가 있을 때") {
        val member = Member.create(
            Email("test@example.com"),
            Password("encodedPassword123"),
            Nickname("홍길동")
        )

        When("deactivate를 호출하면") {
            val deactivatedMember = member.deactivate()

            Then("상태가 INACTIVE로 변경된다") {
                deactivatedMember.status shouldBe MemberStatus.INACTIVE
            }

            Then("isActive()는 false를 반환한다") {
                deactivatedMember.isActive() shouldBe false
            }

            Then("updatedAt이 갱신된다") {
                deactivatedMember.updatedAt shouldNotBe member.updatedAt
            }

            Then("원본 Member는 변경되지 않는다") {
                member.status shouldBe MemberStatus.ACTIVE
            }
        }

        When("닉네임을 변경하면") {
            val newNickname = Nickname("새닉네임")
            val updatedMember = member.updateNickname(newNickname)

            Then("닉네임이 변경된다") {
                updatedMember.nickname shouldBe newNickname
            }

            Then("updatedAt이 갱신된다") {
                updatedMember.updatedAt shouldNotBe member.updatedAt
            }

            Then("원본 Member는 변경되지 않는다") {
                member.nickname.value shouldBe "홍길동"
            }
        }

        When("ID를 변경하면") {
            val newId = MemberId(100L)
            val memberWithNewId = member.withId(newId)

            Then("ID가 변경된다") {
                memberWithNewId.id shouldBe newId
            }

            Then("다른 속성은 그대로 유지된다") {
                memberWithNewId.email shouldBe member.email
                memberWithNewId.password shouldBe member.password
                memberWithNewId.nickname shouldBe member.nickname
                memberWithNewId.status shouldBe member.status
            }
        }
    }
})
