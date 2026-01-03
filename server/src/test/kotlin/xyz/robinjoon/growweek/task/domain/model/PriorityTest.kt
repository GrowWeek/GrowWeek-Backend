package xyz.robinjoon.growweek.task.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class PriorityTest :
    BehaviorSpec({

        Given("할일 중요도를 생성할 때") {

            When("1 이상의 값이 주어지면") {
                val priority = Priority(1)

                Then("중요도가 정상적으로 생성되어야 한다") {
                    priority.value shouldBe 1
                }
            }

            When("큰 값이 주어지면") {
                val priority = Priority(100)

                Then("중요도가 정상적으로 생성되어야 한다") {
                    priority.value shouldBe 100
                }
            }

            When("0이 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        Priority(0)
                    }.message shouldBe "중요도는 1 이상이어야 합니다"
                }
            }

            When("음수가 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        Priority(-1)
                    }.message shouldBe "중요도는 1 이상이어야 합니다"
                }
            }
        }
    })
