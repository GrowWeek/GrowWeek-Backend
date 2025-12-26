package xyz.robinjoon.growweek.task.infrastructure.event

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import xyz.robinjoon.growweek.common.OffsetPage
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.common.domain.UserId
import xyz.robinjoon.growweek.common.event.DefaultDomainEvent
import xyz.robinjoon.growweek.common.event.payload.RetrospectiveEventPayload
import xyz.robinjoon.growweek.task.domain.model.*
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.model.query.TaskQuery
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository
import java.time.LocalDate
import java.time.LocalDateTime

class RetrospectiveCompletedHandlerTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val taskRepository = mockk<TaskRepository>()
    val handler = RetrospectiveCompletedHandler(taskRepository)

    Given("회고 완료 이벤트가 발행되었을 때") {
        val retrospectiveId = RetrospectiveId(1L)
        val userId = UserId(1L)
        val startDate = LocalDate.of(2025, 1, 6)
        val endDate = LocalDate.of(2025, 1, 12)

        val event = DefaultDomainEvent(
            payload = RetrospectiveEventPayload.Completed(
                retrospectiveId = retrospectiveId,
                userId = userId,
                startDate = startDate,
                endDate = endDate
            )
        )

        val now = LocalDateTime.now()

        And("해당 기간에 Task가 있는 경우") {
            val task1 = Task(
                id = TaskId(1L),
                userId = userId,
                title = TaskTitle("할일 1"),
                description = null,
                status = TaskStatus.DONE,
                sensitivityLevel = SensitivityLevel.NONE,
                priority = Priority(1),
                period = TaskPeriod(startDate, endDate.minusDays(1)),
                createdAt = now,
                updatedAt = now
            )
            val task2 = Task(
                id = TaskId(2L),
                userId = userId,
                title = TaskTitle("할일 2"),
                description = null,
                status = TaskStatus.IN_PROGRESS,
                sensitivityLevel = SensitivityLevel.NONE,
                priority = Priority(2),
                period = TaskPeriod(startDate.plusDays(1), endDate),
                createdAt = now,
                updatedAt = now
            )

            val querySlot = slot<TaskQuery>()
            every { taskRepository.findAll(capture(querySlot)) } returns OffsetPage(
                items = listOf(task1, task2),
                page = 0,
                size = Int.MAX_VALUE,
                totalPage = 1
            )

            val commandSlot = slot<List<TaskCommand>>()
            every { taskRepository.saveAll(capture(commandSlot)) } returns listOf(task1, task2)

            When("핸들러가 이벤트를 처리하면") {
                handler.handle(event)

                Then("해당 기간의 Task를 조회해야 한다") {
                    verify(exactly = 1) { taskRepository.findAll(any()) }

                    val capturedQuery = querySlot.captured as TaskQuery.OffsetByUserIdAndWeek
                    capturedQuery.userId shouldBe userId
                    capturedQuery.weekStart shouldBe startDate
                    capturedQuery.weekEnd shouldBe endDate
                }

                Then("모든 Task에 회고를 연결해야 한다") {
                    verify(exactly = 1) { taskRepository.saveAll(any()) }

                    val capturedCommands = commandSlot.captured
                    capturedCommands shouldHaveSize 2

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
            every { taskRepository.findAll(any<TaskQuery>()) } returns OffsetPage(
                items = emptyList(),
                page = 0,
                size = Int.MAX_VALUE,
                totalPage = 0
            )

            When("핸들러가 이벤트를 처리하면") {
                handler.handle(event)

                Then("Task를 조회해야 한다") {
                    verify(exactly = 1) { taskRepository.findAll(any()) }
                }

                Then("saveAll을 호출하지 않아야 한다") {
                    verify(exactly = 0) { taskRepository.saveAll(any()) }
                }
            }
        }
    }

    Given("supports 메서드 테스트") {
        When("RetrospectiveEventPayload.Completed 타입이 전달되면") {
            val result = handler.supports(RetrospectiveEventPayload.Completed::class.java)

            Then("true를 반환해야 한다") {
                result shouldBe true
            }
        }

        When("다른 타입이 전달되면") {
            val result = handler.supports(String::class.java)

            Then("false를 반환해야 한다") {
                result shouldBe false
            }
        }
    }
})
