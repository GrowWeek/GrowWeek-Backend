package xyz.robinjoon.growweek.retrospective.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.UserId
import java.time.LocalDate
import java.time.LocalDateTime

class RetrospectiveTest : BehaviorSpec({

    Given("회고 작성 가능 여부를 확인할 때") {

        When("회고 기간 내이고 상태가 TODO인 경우") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.TODO,
                endDate = LocalDate.now().plusDays(7)
            )

            Then("작성 가능해야 한다") {
                retrospective.canWrite() shouldBe true
            }
        }

        When("회고 기간 내이고 상태가 IN_PROGRESS인 경우") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.IN_PROGRESS,
                endDate = LocalDate.now().plusDays(7)
            )

            Then("작성 가능해야 한다") {
                retrospective.canWrite() shouldBe true
            }
        }

        When("회고 기간 내이고 상태가 AFTER_GENERATE_QUESTION인 경우") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.AFTER_GENERATE_QUESTION,
                endDate = LocalDate.now().plusDays(7)
            )

            Then("작성 가능해야 한다") {
                retrospective.canWrite() shouldBe true
            }
        }

        When("회고 상태가 DONE인 경우") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.DONE,
                endDate = LocalDate.now().plusDays(7)
            )

            Then("작성 불가능해야 한다") {
                retrospective.canWrite() shouldBe false
            }
        }

        When("회고 기간이 지난 경우 (다음 주 월요일 이후)") {
            // endDate가 2주 전이면 다음 주 월요일이 이미 지났으므로 작성 불가
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.IN_PROGRESS,
                endDate = LocalDate.now().minusDays(14)
            )

            Then("작성 불가능해야 한다") {
                retrospective.canWrite() shouldBe false
            }
        }
    }

    Given("질문 생성을 시작할 때") {

        When("상태가 TODO인 경우") {
            val retrospective = createRetrospective(status = RetrospectiveStatus.TODO)

            Then("상태가 BEFORE_GENERATE_QUESTION으로 변경되어야 한다") {
                val updated = retrospective.startGeneratingQuestions()
                updated.status shouldBe RetrospectiveStatus.BEFORE_GENERATE_QUESTION
            }
        }

        When("상태가 TODO가 아닌 경우") {
            val retrospective = createRetrospective(status = RetrospectiveStatus.IN_PROGRESS)

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    retrospective.startGeneratingQuestions()
                }.message shouldBe "질문 생성은 TODO 상태에서만 가능합니다"
            }
        }
    }

    Given("질문 생성을 완료할 때") {
        val retrospectiveId = RetrospectiveId(1)
        val now = LocalDateTime.now()

        When("상태가 BEFORE_GENERATE_QUESTION이고 질문 개수가 일치하는 경우") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.BEFORE_GENERATE_QUESTION,
                questionCount = QuestionCount(3)
            )
            val questions = listOf(
                Question(QuestionId(1), retrospectiveId, "질문 1", 0, now),
                Question(QuestionId(2), retrospectiveId, "질문 2", 1, now),
                Question(QuestionId(3), retrospectiveId, "질문 3", 2, now)
            )

            Then("상태가 AFTER_GENERATE_QUESTION으로 변경되어야 한다") {
                val updated = retrospective.completeQuestionGeneration(questions)
                updated.status shouldBe RetrospectiveStatus.AFTER_GENERATE_QUESTION
                updated.questions.size shouldBe 3
            }
        }

        When("상태가 BEFORE_GENERATE_QUESTION이 아닌 경우") {
            val retrospective = createRetrospective(status = RetrospectiveStatus.TODO)
            val questions = listOf(
                Question(QuestionId(1), retrospectiveId, "질문 1", 0, now),
                Question(QuestionId(2), retrospectiveId, "질문 2", 1, now),
                Question(QuestionId(3), retrospectiveId, "질문 3", 2, now)
            )

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    retrospective.completeQuestionGeneration(questions)
                }.message shouldBe "질문 생성 완료는 BEFORE_GENERATE_QUESTION 상태에서만 가능합니다"
            }
        }

        When("생성된 질문 개수가 설정된 개수와 다른 경우") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.BEFORE_GENERATE_QUESTION,
                questionCount = QuestionCount(3)
            )
            val questions = listOf(
                Question(QuestionId(1), retrospectiveId, "질문 1", 0, now),
                Question(QuestionId(2), retrospectiveId, "질문 2", 1, now)
            )

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    retrospective.completeQuestionGeneration(questions)
                }.message shouldBe "생성된 질문 개수가 설정된 개수와 일치하지 않습니다"
            }
        }
    }

    Given("답변을 작성할 때") {
        val retrospectiveId = RetrospectiveId(1)
        val now = LocalDateTime.now()
        val questions = listOf(
            Question(QuestionId(1), retrospectiveId, "질문 1", 0, now),
            Question(QuestionId(2), retrospectiveId, "질문 2", 1, now),
            Question(QuestionId(3), retrospectiveId, "질문 3", 2, now)
        )

        When("작성 가능한 상태에서 첫 번째 질문에 답변을 작성하면") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.AFTER_GENERATE_QUESTION,
                endDate = LocalDate.now().plusDays(7),
                questions = questions
            )

            Then("상태가 IN_PROGRESS로 변경되고 답변이 저장되어야 한다") {
                val updated = retrospective.writeAnswer(QuestionId(1), "첫 번째 답변")
                updated.status shouldBe RetrospectiveStatus.IN_PROGRESS
                updated.answers[QuestionId(1)] shouldNotBe null
                updated.answers[QuestionId(1)]?.content shouldBe "첫 번째 답변"
            }
        }

        When("IN_PROGRESS 상태에서 두 번째 질문에 답변을 작성하면") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.IN_PROGRESS,
                endDate = LocalDate.now().plusDays(7),
                questions = questions,
                answers = mapOf(
                    QuestionId(1) to Answer(AnswerId(1), QuestionId(1), "첫 번째 답변", now, now)
                )
            )

            Then("답변이 추가되어야 한다 (순서 상관 없음)") {
                val updated = retrospective.writeAnswer(QuestionId(2), "두 번째 답변")
                updated.answers.size shouldBe 2
                updated.answers[QuestionId(2)]?.content shouldBe "두 번째 답변"
            }
        }

        When("세 번째 질문에 먼저 답변을 작성하면 (순서 무관)") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.AFTER_GENERATE_QUESTION,
                endDate = LocalDate.now().plusDays(7),
                questions = questions
            )

            Then("답변이 저장되어야 한다") {
                val updated = retrospective.writeAnswer(QuestionId(3), "세 번째 답변 먼저 작성")
                updated.answers[QuestionId(3)]?.content shouldBe "세 번째 답변 먼저 작성"
            }
        }

        When("일부 질문에만 답변을 작성하면") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.AFTER_GENERATE_QUESTION,
                endDate = LocalDate.now().plusDays(7),
                questions = questions
            )

            Then("일부 질문만 답변이 있어도 된다") {
                val updated = retrospective.writeAnswer(QuestionId(1), "첫 번째 답변만")
                updated.answers.size shouldBe 1
                updated.answers[QuestionId(1)]?.content shouldBe "첫 번째 답변만"
            }
        }

        When("답변을 null로 작성하면 (답변 안함)") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.AFTER_GENERATE_QUESTION,
                endDate = LocalDate.now().plusDays(7),
                questions = questions
            )

            Then("null 답변이 저장되어야 한다") {
                val updated = retrospective.writeAnswer(QuestionId(1), null)
                updated.answers[QuestionId(1)]?.content shouldBe null
            }
        }

        When("기존 답변을 수정하면") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.IN_PROGRESS,
                endDate = LocalDate.now().plusDays(7),
                questions = questions,
                answers = mapOf(
                    QuestionId(1) to Answer(AnswerId(1), QuestionId(1), "기존 답변", now, now)
                )
            )

            Then("답변이 수정되어야 한다") {
                val updated = retrospective.writeAnswer(QuestionId(1), "수정된 답변")
                updated.answers[QuestionId(1)]?.content shouldBe "수정된 답변"
            }
        }

        When("존재하지 않는 질문에 답변을 작성하려고 하면") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.AFTER_GENERATE_QUESTION,
                endDate = LocalDate.now().plusDays(7),
                questions = questions
            )

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    retrospective.writeAnswer(QuestionId(999), "존재하지 않는 질문")
                }.message shouldBe "존재하지 않는 질문입니다"
            }
        }

        When("작성 기간이 지난 경우") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.IN_PROGRESS,
                endDate = LocalDate.now().minusDays(14),
                questions = questions
            )

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    retrospective.writeAnswer(QuestionId(1), "기간 지남")
                }.message shouldBe "회고 작성 기간이 지났거나 이미 완료된 회고입니다"
            }
        }

        When("이미 완료된 회고에 답변을 작성하려고 하면") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.DONE,
                endDate = LocalDate.now().plusDays(7),
                questions = questions
            )

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    retrospective.writeAnswer(QuestionId(1), "완료된 회고")
                }.message shouldBe "회고 작성 기간이 지났거나 이미 완료된 회고입니다"
            }
        }
    }

    Given("기타 회고 내용을 작성할 때") {

        When("작성 가능한 상태에서 내용을 작성하면") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.IN_PROGRESS,
                endDate = LocalDate.now().plusDays(7)
            )
            val notes = AdditionalNotes("이번 주는 정말 바빴습니다.")

            Then("내용이 저장되어야 한다") {
                val updated = retrospective.writeAdditionalNotes(notes)
                updated.additionalNotes shouldBe notes
            }
        }

        When("작성 기간이 지난 경우") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.IN_PROGRESS,
                endDate = LocalDate.now().minusDays(14)
            )
            val notes = AdditionalNotes("기간 지남")

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    retrospective.writeAdditionalNotes(notes)
                }.message shouldBe "회고 작성 기간이 지났거나 이미 완료된 회고입니다"
            }
        }

        When("이미 완료된 회고에 내용을 작성하려고 하면") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.DONE,
                endDate = LocalDate.now().plusDays(7)
            )
            val notes = AdditionalNotes("완료된 회고")

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    retrospective.writeAdditionalNotes(notes)
                }.message shouldBe "회고 작성 기간이 지났거나 이미 완료된 회고입니다"
            }
        }
    }

    Given("회고를 완료할 때") {
        val retrospectiveId = RetrospectiveId(1)
        val now = LocalDateTime.now()
        val questions = listOf(
            Question(QuestionId(1), retrospectiveId, "질문 1", 0, now),
            Question(QuestionId(2), retrospectiveId, "질문 2", 1, now),
            Question(QuestionId(3), retrospectiveId, "질문 3", 2, now)
        )

        When("상태가 IN_PROGRESS인 경우") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.IN_PROGRESS,
                questions = questions,
                answers = mapOf(
                    QuestionId(1) to Answer(AnswerId(1), QuestionId(1), "답변 1", now, now)
                )
            )

            Then("상태가 DONE으로 변경되어야 한다") {
                val updated = retrospective.complete()
                updated.status shouldBe RetrospectiveStatus.DONE
            }
        }

        When("상태가 IN_PROGRESS가 아닌 경우") {
            val retrospective = createRetrospective(
                status = RetrospectiveStatus.AFTER_GENERATE_QUESTION,
                questions = questions
            )

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    retrospective.complete()
                }.message shouldBe "답변을 하나 이상 작성한 후 완료할 수 있습니다"
            }
        }

        When("상태가 TODO인 경우") {
            val retrospective = createRetrospective(status = RetrospectiveStatus.TODO)

            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    retrospective.complete()
                }.message shouldBe "답변을 하나 이상 작성한 후 완료할 수 있습니다"
            }
        }
    }
})

private fun createRetrospective(
    status: RetrospectiveStatus = RetrospectiveStatus.TODO,
    endDate: LocalDate = LocalDate.now().plusDays(7),
    questionCount: QuestionCount = QuestionCount.DEFAULT,
    questions: List<Question> = emptyList(),
    answers: Map<QuestionId, Answer> = emptyMap()
): Retrospective {
    val now = LocalDateTime.now()
    return Retrospective(
        id = RetrospectiveId(1),
        userId = UserId(1L),
        period = RetrospectivePeriod(endDate.minusDays(6), endDate),
        status = status,
        questionCount = questionCount,
        questions = questions,
        answers = answers,
        additionalNotes = null,
        createdAt = now,
        updatedAt = now
    )
}
