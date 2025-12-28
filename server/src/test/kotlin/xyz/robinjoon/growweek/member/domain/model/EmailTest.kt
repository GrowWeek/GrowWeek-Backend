package xyz.robinjoon.growweek.member.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class EmailTest : BehaviorSpec({

    Given("이메일 Value Object 생성 시") {

        When("유효한 이메일 형식이면") {
            val email = Email("test@example.com")

            Then("정상적으로 생성된다") {
                email.value shouldBe "test@example.com"
            }
        }

        When("대소문자가 섞인 유효한 이메일이면") {
            val email = Email("Test.User@Example.COM")

            Then("정상적으로 생성된다") {
                email.value shouldBe "Test.User@Example.COM"
            }
        }

        When("비어있는 이메일이면") {
            Then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Email("")
                }.message shouldBe "이메일은 비어있을 수 없습니다"
            }
        }

        When("공백만 있는 이메일이면") {
            Then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Email("   ")
                }.message shouldBe "이메일은 비어있을 수 없습니다"
            }
        }

        When("@가 없는 이메일이면") {
            Then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Email("invalid-email")
                }.message shouldBe "올바른 이메일 형식이 아닙니다"
            }
        }

        When("도메인이 없는 이메일이면") {
            Then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Email("test@")
                }.message shouldBe "올바른 이메일 형식이 아닙니다"
            }
        }

        When("로컬 파트가 없는 이메일이면") {
            Then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Email("@example.com")
                }.message shouldBe "올바른 이메일 형식이 아닙니다"
            }
        }

        When("100자를 초과하는 이메일이면") {
            val longEmail = "a".repeat(90) + "@example.com"

            Then("예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Email(longEmail)
                }.message shouldBe "이메일은 100자 이하여야 합니다"
            }
        }

        When("정확히 100자인 유효한 이메일이면") {
            val exactEmail = "a".repeat(88) + "@example.com" // 88 + 12 = 100

            Then("정상적으로 생성된다") {
                val email = Email(exactEmail)
                email.value.length shouldBe 100
            }
        }
    }
})
