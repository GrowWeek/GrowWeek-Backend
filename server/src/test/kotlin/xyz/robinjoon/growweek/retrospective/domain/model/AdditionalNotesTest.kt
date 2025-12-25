package xyz.robinjoon.growweek.retrospective.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class AdditionalNotesTest : BehaviorSpec({

    Given("기타 회고 내용을 생성할 때") {

        When("유효한 내용이 주어지면") {
            val notes = AdditionalNotes("이번 주 회고에서 느낀 점입니다.")

            Then("내용이 정상적으로 생성되어야 한다") {
                notes.value shouldBe "이번 주 회고에서 느낀 점입니다."
            }
        }

        When("빈 내용이 주어지면") {
            val notes = AdditionalNotes("")

            Then("내용이 정상적으로 생성되어야 한다") {
                notes.value shouldBe ""
            }
        }

        When("3000자 이하의 내용이 주어지면") {
            val threeThousandCharContent = "가".repeat(3000)
            val notes = AdditionalNotes(threeThousandCharContent)

            Then("내용이 정상적으로 생성되어야 한다") {
                notes.value.length shouldBe 3000
            }
        }

        When("3000자를 초과하는 내용이 주어지면") {
            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    AdditionalNotes("가".repeat(3001))
                }.message shouldBe "기타 회고 내용은 3000자를 초과할 수 없습니다"
            }
        }
    }
})
