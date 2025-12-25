package xyz.robinjoon.growweek.task.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class TaskDescriptionTest : BehaviorSpec({

    Given("할일 상세 설명을 생성할 때") {

        When("유효한 설명이 주어지면") {
            val description = TaskDescription("할 일에 대한 상세 설명입니다.")

            Then("설명이 정상적으로 생성되어야 한다") {
                description.value shouldBe "할 일에 대한 상세 설명입니다."
            }
        }

        When("빈 설명이 주어지면") {
            val description = TaskDescription("")

            Then("설명이 정상적으로 생성되어야 한다") {
                description.value shouldBe ""
            }
        }

        When("3000자 이하의 설명이 주어지면") {
            val threeThousandCharDescription = "가".repeat(3000)
            val description = TaskDescription(threeThousandCharDescription)

            Then("설명이 정상적으로 생성되어야 한다") {
                description.value.length shouldBe 3000
            }
        }

        When("3000자를 초과하는 설명이 주어지면") {
            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    TaskDescription("가".repeat(3001))
                }.message shouldBe "설명은 3000자 이하여야 합니다"
            }
        }
    }
})
