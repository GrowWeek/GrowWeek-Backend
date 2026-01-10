package xyz.robinjoon.growweek.common.event

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.context.ApplicationEventPublisher
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.common.event.payload.RetrospectiveEventPayload
import xyz.robinjoon.growweek.common.infrastructure.SpringDomainEventPublisher
import xyz.robinjoon.growweek.retrospective.application.command.RetrospectiveApplicationCommand
import xyz.robinjoon.growweek.retrospective.application.service.CompleteRetrospectiveService
import xyz.robinjoon.growweek.retrospective.domain.model.*
import xyz.robinjoon.growweek.retrospective.domain.model.Answer
import xyz.robinjoon.growweek.retrospective.domain.model.command.RetrospectiveCommand
import xyz.robinjoon.growweek.retrospective.domain.repository.RetrospectiveRepository
import xyz.robinjoon.growweek.task.domain.model.*
import xyz.robinjoon.growweek.task.domain.model.command.CompletedWeekCommand
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.CompletedWeekRepository
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository
import xyz.robinjoon.growweek.task.infrastructure.event.RetrospectiveCompletedHandler
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 회고 완료 → Task 연결 통합 테스트
 *
 * 이 테스트는 다음 흐름을 검증합니다:
 * 1. CompleteRetrospectiveService가 회고를 완료하고 이벤트를 발행
 * 2. RetrospectiveCompletedHandler가 이벤트를 수신
 * 3. Handler가 해당 기간의 Task들에 retrospectiveId를 연결
 */
class RetrospectiveCompletedIntegrationTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        Given("회고 완료 시 Task 연결 통합 테스트") {
            val retrospectiveRepository = mockk<RetrospectiveRepository>()
            val taskRepository = mockk<TaskRepository>()
            val completedWeekRepository = mockk<CompletedWeekRepository>()

            // Handler 생성
            val handler = RetrospectiveCompletedHandler(taskRepository, completedWeekRepository)

            // 이벤트 캡처를 위한 slot
            val eventSlot = slot<Any>()
            val applicationEventPublisher = mockk<ApplicationEventPublisher>()
            every { applicationEventPublisher.publishEvent(capture(eventSlot)) } just Runs

            val eventPublisher = SpringDomainEventPublisher(applicationEventPublisher)
            val service = CompleteRetrospectiveService(retrospectiveRepository, eventPublisher)

            val memberId = MemberId(1L)
            val retrospectiveId = RetrospectiveId(1L)
            val startDate = LocalDate.of(2025, 1, 6)
            val endDate = LocalDate.of(2025, 1, 12)
            val weekId = WeekId.of(startDate)
            val now = LocalDateTime.now()

            // 회고 데이터 설정
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

            val completedRetrospective =
                Retrospective(
                    id = retrospectiveId,
                    memberId = memberId,
                    period = RetrospectivePeriod(startDate, endDate),
                    status = RetrospectiveStatus.DONE,
                    questionCount = QuestionCount(3),
                    questions = listOf(question),
                    answers = mapOf(questionId to answer),
                    additionalNotes = null,
                    createdAt = now,
                    updatedAt = now,
                )

            // Task 데이터 설정
            val task1 =
                Task(
                    id = TaskId(1L),
                    memberId = memberId,
                    title = TaskTitle("할일 1"),
                    description = null,
                    status = TaskStatus.DONE,
                    sensitivityLevel = SensitivityLevel.NONE,
                    priority = Priority(1),
                    weekId = weekId,
                    dueDate = endDate.minusDays(1),
                    createdAt = now,
                    updatedAt = now,
                )
            val task2 =
                Task(
                    id = TaskId(2L),
                    memberId = memberId,
                    title = TaskTitle("할일 2"),
                    description = null,
                    status = TaskStatus.IN_PROGRESS,
                    sensitivityLevel = SensitivityLevel.NONE,
                    priority = Priority(2),
                    weekId = weekId,
                    dueDate = endDate,
                    createdAt = now,
                    updatedAt = now,
                )

            And("해당 기간에 Task가 존재하는 경우") {
                val retrospectiveCommandSlot = slot<List<RetrospectiveCommand>>()
                every { retrospectiveRepository.saveAll(capture(retrospectiveCommandSlot)) } returns
                    listOf(
                        completedRetrospective,
                    )

                every {
                    completedWeekRepository.saveAll(any<List<CompletedWeekCommand>>())
                } returns
                    listOf(
                        CompletedWeek(
                            retrospectiveId = retrospectiveId,
                            memberId = memberId,
                            weekId = weekId,
                            completedAt = now,
                        ),
                    )

                every { taskRepository.findAll(any<TaskQuery>()) } returns
                    OffsetPage(
                        items = listOf(task1, task2),
                        page = 0,
                        size = Int.MAX_VALUE,
                        totalPage = 1,
                    )

                val taskCommandSlot = slot<List<TaskCommand>>()
                every { taskRepository.saveAll(capture(taskCommandSlot)) } returns listOf(task1, task2)

                When("회고 완료 서비스를 실행하면") {
                    val command =
                        RetrospectiveApplicationCommand.CompleteRetrospective(
                            retrospectiveId = retrospectiveId,
                            memberId = memberId,
                        )

                    val result = service.execute(command)

                    // 발행된 이벤트를 핸들러로 전달
                    @Suppress("UNCHECKED_CAST")
                    val publishedEvent = eventSlot.captured as DomainEvent<RetrospectiveEventPayload.Completed>
                    handler.handle(publishedEvent)

                    Then("회고가 완료되어야 한다") {
                        result.id shouldBe retrospectiveId
                        result.status shouldBe RetrospectiveStatus.DONE
                    }

                    Then("이벤트가 발행되어야 한다") {
                        eventSlot.isCaptured shouldBe true
                        val event = eventSlot.captured as DomainEvent<*>
                        event.payload shouldBe
                            RetrospectiveEventPayload.Completed(
                                retrospectiveId = retrospectiveId,
                                memberId = memberId,
                                startDate = startDate,
                                endDate = endDate,
                            )
                    }

                    Then("Task 조회가 실행되어야 한다") {
                        verify(exactly = 1) { taskRepository.findAll(any<TaskQuery>()) }
                    }

                    Then("모든 Task에 회고가 연결되어야 한다") {
                        verify(exactly = 1) { taskRepository.saveAll(any()) }

                        val capturedCommands = taskCommandSlot.captured
                        capturedCommands.size shouldBe 2

                        val linkCommand1 = capturedCommands[0] as TaskCommand.LinkRetrospective
                        linkCommand1.taskId shouldBe TaskId(1L)
                        linkCommand1.retrospectiveId shouldBe retrospectiveId

                        val linkCommand2 = capturedCommands[1] as TaskCommand.LinkRetrospective
                        linkCommand2.taskId shouldBe TaskId(2L)
                        linkCommand2.retrospectiveId shouldBe retrospectiveId
                    }
                }
            }

            And("해당 기간에 Task가 없는 경우") {
                every { retrospectiveRepository.saveAll(any()) } returns listOf(completedRetrospective)

                every {
                    completedWeekRepository.saveAll(any<List<CompletedWeekCommand>>())
                } returns
                    listOf(
                        CompletedWeek(
                            retrospectiveId = retrospectiveId,
                            memberId = memberId,
                            weekId = weekId,
                            completedAt = now,
                        ),
                    )

                every { taskRepository.findAll(any<TaskQuery>()) } returns
                    OffsetPage(
                        items = emptyList(),
                        page = 0,
                        size = Int.MAX_VALUE,
                        totalPage = 0,
                    )

                When("회고 완료 서비스를 실행하면") {
                    val command =
                        RetrospectiveApplicationCommand.CompleteRetrospective(
                            retrospectiveId = retrospectiveId,
                            memberId = memberId,
                        )

                    val result = service.execute(command)

                    // 발행된 이벤트를 핸들러로 전달
                    @Suppress("UNCHECKED_CAST")
                    val publishedEvent = eventSlot.captured as DomainEvent<RetrospectiveEventPayload.Completed>
                    handler.handle(publishedEvent)

                    Then("회고가 완료되어야 한다") {
                        result.id shouldBe retrospectiveId
                        result.status shouldBe RetrospectiveStatus.DONE
                    }

                    Then("이벤트가 발행되어야 한다") {
                        eventSlot.isCaptured shouldBe true
                    }

                    Then("Task 조회가 실행되어야 한다") {
                        verify(exactly = 1) { taskRepository.findAll(any<TaskQuery>()) }
                    }

                    Then("saveAll이 호출되지 않아야 한다") {
                        verify(exactly = 0) { taskRepository.saveAll(any()) }
                    }
                }
            }
        }
    })
