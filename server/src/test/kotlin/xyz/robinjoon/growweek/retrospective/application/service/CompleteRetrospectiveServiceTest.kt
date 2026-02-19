package xyz.robinjoon.growweek.retrospective.application.service

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import xyz.robinjoon.growweek.common.contract.retrospective.RetrospectiveEventPayload
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.common.event.DomainEventPublisher
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.domain.model.*
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository
import java.time.LocalDateTime

class CompleteRetrospectiveServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        val retrospectiveRepository = mockk<RetrospectiveRepository>()
        val eventPublisher = mockk<DomainEventPublisher>(relaxed = true)
        val service = CompleteRetrospectiveService(retrospectiveRepository, eventPublisher)

        Given("회고 완료 요청이 왔을 때") {
            val retrospectiveId = RetrospectiveId(1L)
            val memberId = MemberId(1L)

            val command =
                RetrospectiveApplicationCommand.CompleteRetrospective(
                    retrospectiveId = retrospectiveId,
                    memberId = memberId,
                )

            val now = LocalDateTime.now()
            val questionId = QuestionId(1L)
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
                    content = "좋았습니다",
                    createdAt = now,
                    updatedAt = now,
                )

            val weekId = WeekId.of(2025, 2) // 2025-W02

            val completedRetrospective =
                Retrospective(
                    id = retrospectiveId,
                    memberId = memberId,
                    weekId = weekId,
                    status = RetrospectiveStatus.DONE,
                    questionCount = QuestionCount(3),
                    questions = listOf(question),
                    answers = mapOf(questionId to answer),
                    additionalNotes = null,
                    createdAt = now,
                    updatedAt = now,
                )

            val commandSlot = slot<List<RetrospectiveCommand>>()
            every { retrospectiveRepository.saveAll(capture(commandSlot)) } returns listOf(completedRetrospective)

            When("서비스를 실행하면") {
                val result = service.execute(command)

                Then("Repository에 저장 요청을 해야 한다") {
                    verify(exactly = 1) { retrospectiveRepository.saveAll(any()) }
                }

                Then("Application Command가 Domain Command로 변환되어야 한다") {
                    val capturedCommand = commandSlot.captured.first() as RetrospectiveCommand.CompleteRetrospective
                    capturedCommand.retrospectiveId shouldBe retrospectiveId
                    capturedCommand.memberId shouldBe memberId
                }

                Then("완료된 회고 DTO를 반환해야 한다") {
                    result.id shouldBe retrospectiveId
                    result.status shouldBe RetrospectiveStatus.DONE
                }

                Then("회고 완료 이벤트를 발행해야 한다") {
                    val eventSlot = slot<RetrospectiveEventPayload.Completed>()
                    verify(exactly = 1) { eventPublisher.publish(capture(eventSlot)) }

                    val capturedPayload = eventSlot.captured
                    capturedPayload.retrospectiveId shouldBe retrospectiveId
                    capturedPayload.memberId shouldBe memberId
                    capturedPayload.weekId shouldBe weekId
                }
            }
        }
    })
