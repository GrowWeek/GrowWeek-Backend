package xyz.robinjoon.growweek.member.domain.model

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PasswordTest :
    BehaviorSpec({

        Given("Password Value Object 생성 시") {

            When("유효한 암호화된 비밀번호가 주어지면") {
                val encodedPassword = "\$2a\$10\$abcdefghijklmnopqrstuv"

                Then("정상적으로 생성된다") {
                    val password = Password(encodedPassword)
                    password.value shouldBe encodedPassword
                }
            }

            When("비어있는 비밀번호면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        Password("")
                    }.message shouldBe "비밀번호는 비어있을 수 없습니다"
                }
            }

            When("공백만 있는 비밀번호면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        Password("   ")
                    }.message shouldBe "비밀번호는 비어있을 수 없습니다"
                }
            }
        }

        Given("비밀번호 유효성 검사 시") {

            When("8자 이상 100자 이하의 비밀번호면") {
                Then("정상적으로 통과한다") {
                    shouldNotThrowAny {
                        Password.validate("password123")
                    }
                }
            }

            When("정확히 8자인 비밀번호면") {
                Then("정상적으로 통과한다") {
                    shouldNotThrowAny {
                        Password.validate("12345678")
                    }
                }
            }

            When("정확히 100자인 비밀번호면") {
                val longPassword = "a".repeat(100)

                Then("정상적으로 통과한다") {
                    shouldNotThrowAny {
                        Password.validate(longPassword)
                    }
                }
            }

            When("8자 미만의 비밀번호면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        Password.validate("1234567")
                    }.message shouldBe "비밀번호는 8자 이상이어야 합니다"
                }
            }

            When("100자 초과의 비밀번호면") {
                val tooLongPassword = "a".repeat(101)

                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        Password.validate(tooLongPassword)
                    }.message shouldBe "비밀번호는 100자 이하여야 합니다"
                }
            }
        }
    })
