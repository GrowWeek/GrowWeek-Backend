package xyz.robinjoon.growweek.retrospective.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class QuestionCountTest : BehaviorSpec({

    Given("질문 개수를 생성할 때") {

        When("최소값(2개)이 주어지면") {
            val count = QuestionCount(2)

            Then("질문 개수가 정상적으로 생성되어야 한다") {
                count.value shouldBe 2
            }
        }

        When("최대값(7개)이 주어지면") {
            val count = QuestionCount(7)

            Then("질문 개수가 정상적으로 생성되어야 한다") {
                count.value shouldBe 7
            }
        }

        When("기본값(3개)이 주어지면") {
            val count = QuestionCount.DEFAULT

            Then("기본 질문 개수가 3이어야 한다") {
                count.value shouldBe 3
            }
        }

        When("범위 내의 값(5개)이 주어지면") {
            val count = QuestionCount(5)

            Then("질문 개수가 정상적으로 생성되어야 한다") {
                count.value shouldBe 5
            }
        }

        When("최소값 미만(1개)이 주어지면") {
            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    QuestionCount(1)
                }.message shouldBe "질문 개수는 최소 2개, 최대 7개여야 합니다"
            }
        }

        When("0개가 주어지면") {
            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    QuestionCount(0)
                }.message shouldBe "질문 개수는 최소 2개, 최대 7개여야 합니다"
            }
        }

        When("최대값 초과(8개)가 주어지면") {
            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    QuestionCount(8)
                }.message shouldBe "질문 개수는 최소 2개, 최대 7개여야 합니다"
            }
        }

        When("음수가 주어지면") {
            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    QuestionCount(-1)
                }.message shouldBe "질문 개수는 최소 2개, 최대 7개여야 합니다"
            }
        }
    }
})
