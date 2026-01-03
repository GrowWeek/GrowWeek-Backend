package xyz.robinjoon.growweek.member.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class NicknameTest :
    BehaviorSpec({

        Given("닉네임 Value Object 생성 시") {

            When("유효한 닉네임이 주어지면") {
                val nickname = Nickname("홍길동")

                Then("정상적으로 생성된다") {
                    nickname.value shouldBe "홍길동"
                }
            }

            When("영문 닉네임이 주어지면") {
                val nickname = Nickname("JohnDoe")

                Then("정상적으로 생성된다") {
                    nickname.value shouldBe "JohnDoe"
                }
            }

            When("숫자가 포함된 닉네임이 주어지면") {
                val nickname = Nickname("User123")

                Then("정상적으로 생성된다") {
                    nickname.value shouldBe "User123"
                }
            }

            When("비어있는 닉네임이면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        Nickname("")
                    }.message shouldBe "닉네임은 비어있을 수 없습니다"
                }
            }

            When("공백만 있는 닉네임이면") {
                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        Nickname("   ")
                    }.message shouldBe "닉네임은 비어있을 수 없습니다"
                }
            }

            When("50자를 초과하는 닉네임이면") {
                val longNickname = "가".repeat(51)

                Then("예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        Nickname(longNickname)
                    }.message shouldBe "닉네임은 50자 이하여야 합니다"
                }
            }

            When("정확히 50자인 닉네임이면") {
                val exactNickname = "가".repeat(50)

                Then("정상적으로 생성된다") {
                    val nickname = Nickname(exactNickname)
                    nickname.value.length shouldBe 50
                }
            }
        }
    })
