package xyz.robinjoon.growweek.retrospective.application.service

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.domain.model.*
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository
import java.time.LocalDate
import java.time.LocalDateTime

class WriteAnswerServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        val retrospectiveRepository = mockk<RetrospectiveRepository>()
        val service = WriteAnswerService(retrospectiveRepository)

        Given("답변 작성 요청이 왔을 때") {
            val retrospectiveId = RetrospectiveId(1L)
            val memberId = MemberId(1L)
            val questionId = QuestionId(1L)
            val answerContent = "이번 주는 생산적이었습니다."

            val command =
                RetrospectiveApplicationCommand.WriteAnswer(
                    retrospectiveId = retrospectiveId,
                    memberId = memberId,
                    questionId = questionId,
                    content = answerContent,
                )

            val now = LocalDateTime.now()
            val question =
                Question(
                    id = questionId,
                    retrospectiveId = retrospectiveId,
                    content = "이번 주는 어떠셨나요?",
                    order = 0,
                    createdAt = now,
                )

            val answer =
                Answer(
                    id = AnswerId(1L),
                    questionId = questionId,
                    content = answerContent,
                    createdAt = now,
                    updatedAt = now,
                )

            val updatedRetrospective =
                Retrospective(
                    id = retrospectiveId,
                    memberId = memberId,
                    period = RetrospectivePeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12)),
                    status = RetrospectiveStatus.IN_PROGRESS,
                    questionCount = QuestionCount(3),
                    questions = listOf(question),
                    answers = mapOf(questionId to answer),
                    additionalNotes = null,
                    createdAt = now,
                    updatedAt = now,
                )

            val commandSlot = slot<List<RetrospectiveCommand>>()
            every { retrospectiveRepository.saveAll(capture(commandSlot)) } returns listOf(updatedRetrospective)

            When("서비스를 실행하면") {
                val result = service.execute(command)

                Then("Repository에 저장 요청을 해야 한다") {
                    verify(exactly = 1) { retrospectiveRepository.saveAll(any()) }
                }

                Then("Application Command가 Domain Command로 변환되어야 한다") {
                    val capturedCommand = commandSlot.captured.first() as RetrospectiveCommand.WriteAnswer
                    capturedCommand.retrospectiveId shouldBe retrospectiveId
                    capturedCommand.memberId shouldBe memberId
                    capturedCommand.questionId shouldBe questionId
                    capturedCommand.content shouldBe answerContent
                }

                Then("답변이 작성된 회고 DTO를 반환해야 한다") {
                    result.id shouldBe retrospectiveId
                    result.status shouldBe RetrospectiveStatus.IN_PROGRESS
                    result.answers.size shouldBe 1
                    result.answers.first().content shouldBe answerContent
                }
            }
        }

        Given("답변을 null로 수정 요청이 왔을 때") {
            val retrospectiveId = RetrospectiveId(1L)
            val memberId = MemberId(1L)
            val questionId = QuestionId(1L)

            val command =
                RetrospectiveApplicationCommand.WriteAnswer(
                    retrospectiveId = retrospectiveId,
                    memberId = memberId,
                    questionId = questionId,
                    content = null,
                )

            val now = LocalDateTime.now()
            val question =
                Question(
                    id = questionId,
                    retrospectiveId = retrospectiveId,
                    content = "이번 주는 어떠셨나요?",
                    order = 0,
                    createdAt = now,
                )

            val answer =
                Answer(
                    id = AnswerId(1L),
                    questionId = questionId,
                    content = null,
                    createdAt = now,
                    updatedAt = now,
                )

            val updatedRetrospective =
                Retrospective(
                    id = retrospectiveId,
                    memberId = memberId,
                    period = RetrospectivePeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12)),
                    status = RetrospectiveStatus.IN_PROGRESS,
                    questionCount = QuestionCount(3),
                    questions = listOf(question),
                    answers = mapOf(questionId to answer),
                    additionalNotes = null,
                    createdAt = now,
                    updatedAt = now,
                )

            val commandSlot = slot<List<RetrospectiveCommand>>()
            every { retrospectiveRepository.saveAll(capture(commandSlot)) } returns listOf(updatedRetrospective)

            When("서비스를 실행하면") {
                val result = service.execute(command)

                Then("content가 null로 전달되어야 한다") {
                    val capturedCommand = commandSlot.captured.first() as RetrospectiveCommand.WriteAnswer
                    capturedCommand.content shouldBe null
                }

                Then("반환된 답변의 content가 null이어야 한다") {
                    result.answers.first().content shouldBe null
                }
            }
        }
    })
