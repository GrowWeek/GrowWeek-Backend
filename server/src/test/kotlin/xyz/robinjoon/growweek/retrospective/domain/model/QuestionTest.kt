package xyz.robinjoon.growweek.retrospective.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import java.time.LocalDateTime

class QuestionTest :
    BehaviorSpec({

        Given("질문을 생성할 때") {
            val now = LocalDateTime.now()
            val retrospectiveId = RetrospectiveId(1)

            When("유효한 질문 내용과 순서가 주어지면") {
                val question =
                    Question(
                        id = QuestionId(1),
                        retrospectiveId = retrospectiveId,
                        content = "이번 주 가장 잘한 일은 무엇인가요?",
                        order = 0,
                        createdAt = now,
                    )

                Then("질문이 정상적으로 생성되어야 한다") {
                    question.content shouldBe "이번 주 가장 잘한 일은 무엇인가요?"
                    question.order shouldBe 0
                }
            }

            When("순서가 0인 경우") {
                val question =
                    Question(
                        id = QuestionId(1),
                        retrospectiveId = retrospectiveId,
                        content = "첫 번째 질문",
                        order = 0,
                        createdAt = now,
                    )

                Then("질문이 정상적으로 생성되어야 한다") {
                    question.order shouldBe 0
                }
            }

            When("순서가 양수인 경우") {
                val question =
                    Question(
                        id = QuestionId(1),
                        retrospectiveId = retrospectiveId,
                        content = "두 번째 질문",
                        order = 1,
                        createdAt = now,
                    )

                Then("질문이 정상적으로 생성되어야 한다") {
                    question.order shouldBe 1
                }
            }

            When("빈 질문 내용이 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        Question(
                            id = QuestionId(1),
                            retrospectiveId = retrospectiveId,
                            content = "",
                            order = 0,
                            createdAt = now,
                        )
                    }.message shouldBe "질문 내용은 비어있을 수 없습니다"
                }
            }

            When("공백만 있는 질문 내용이 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        Question(
                            id = QuestionId(1),
                            retrospectiveId = retrospectiveId,
                            content = "   ",
                            order = 0,
                            createdAt = now,
                        )
                    }.message shouldBe "질문 내용은 비어있을 수 없습니다"
                }
            }

            When("음수 순서가 주어지면") {
                Then("예외가 발생해야 한다") {
                    shouldThrow<IllegalArgumentException> {
                        Question(
                            id = QuestionId(1),
                            retrospectiveId = retrospectiveId,
                            content = "질문 내용",
                            order = -1,
                            createdAt = now,
                        )
                    }.message shouldBe "질문 순서는 0 이상이어야 합니다"
                }
            }
        }
    })
