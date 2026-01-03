package xyz.robinjoon.growweek.task.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class TaskTitleTest :
    BehaviorSpec({

        Given("할일 제목을 생성할 때") {

            When("유효한 제목이 주어지면") {
                val title = TaskTitle("할 일 제목")

                Then("제목이 정상적으로 생성되어야 한다") {
                    title.value shouldBe "할 일 제목"
                }
            }

            When("50자 이하의 제목이 주어지면") {
                val fiftyCharTitle = "가".repeat(50)
                val title = TaskTitle(fiftyCharTitle)

                Then("제목이 정상적으로 생성되어야 한다") {
                    title.value.length shouldBe 50
                }
            }

            When("빈 제목이 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        TaskTitle("")
                    }.message shouldBe "제목은 비어있을 수 없습니다"
                }
            }

            When("공백만 있는 제목이 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        TaskTitle("   ")
                    }.message shouldBe "제목은 비어있을 수 없습니다"
                }
            }

            When("50자를 초과하는 제목이 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        TaskTitle("가".repeat(51))
                    }.message shouldBe "제목은 50자 이하여야 합니다"
                }
            }
        }
    })
