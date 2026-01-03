package xyz.robinjoon.growweek.task.application.service

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand
import xyz.robinjoon.growweek.task.domain.model.*
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository
import java.time.LocalDate
import java.time.LocalDateTime

class UpdateTaskStatusServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val taskRepository = mockk<TaskRepository>()
    val service = UpdateTaskStatusService(taskRepository)

    Given("할일 상태 변경 요청이 왔을 때") {
        val taskId = TaskId(1L)
        val memberId = MemberId(1L)
        val newStatus = TaskStatus.IN_PROGRESS

        val command = TaskApplicationCommand.UpdateTaskStatus(
            taskId = taskId,
            memberId = memberId,
            status = newStatus
        )

        val updatedTask = Task(
            id = taskId,
            memberId = memberId,
            title = TaskTitle("테스트 할일"),
            description = null,
            status = newStatus,
            sensitivityLevel = SensitivityLevel.NONE,
            priority = Priority(1),
            period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12)),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            retrospectiveId = null
        )

        val commandSlot = slot<List<TaskCommand>>()
        every { taskRepository.saveAll(capture(commandSlot)) } returns listOf(updatedTask)

        When("서비스를 실행하면") {
            val result = service.execute(command)

            Then("Repository에 저장 요청을 해야 한다") {
                verify(exactly = 1) { taskRepository.saveAll(any()) }
            }

            Then("Application Command가 Domain Command로 변환되어야 한다") {
                val capturedCommand = commandSlot.captured.first() as TaskCommand.UpdateTaskStatus
                capturedCommand.taskId shouldBe taskId
                capturedCommand.memberId shouldBe memberId
                capturedCommand.status shouldBe newStatus
            }

            Then("상태가 변경된 Task의 DTO를 반환해야 한다") {
                result.id shouldBe taskId
                result.status shouldBe newStatus
            }
        }
    }

    Given("TODO에서 DONE으로 상태 변경 요청이 왔을 때") {
        val taskId = TaskId(2L)
        val memberId = MemberId(1L)

        val command = TaskApplicationCommand.UpdateTaskStatus(
            taskId = taskId,
            memberId = memberId,
            status = TaskStatus.DONE
        )

        val updatedTask = Task(
            id = taskId,
            memberId = memberId,
            title = TaskTitle("완료된 할일"),
            description = null,
            status = TaskStatus.DONE,
            sensitivityLevel = SensitivityLevel.NONE,
            priority = Priority(1),
            period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12)),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            retrospectiveId = null
        )

        every { taskRepository.saveAll(any()) } returns listOf(updatedTask)

        When("서비스를 실행하면") {
            val result = service.execute(command)

            Then("상태가 DONE으로 변경되어야 한다") {
                result.status shouldBe TaskStatus.DONE
            }
        }
    }

    Given("CANCEL 상태로 변경 요청이 왔을 때") {
        val taskId = TaskId(3L)
        val memberId = MemberId(1L)

        val command = TaskApplicationCommand.UpdateTaskStatus(
            taskId = taskId,
            memberId = memberId,
            status = TaskStatus.CANCEL
        )

        val updatedTask = Task(
            id = taskId,
            memberId = memberId,
            title = TaskTitle("취소된 할일"),
            description = null,
            status = TaskStatus.CANCEL,
            sensitivityLevel = SensitivityLevel.NONE,
            priority = Priority(1),
            period = TaskPeriod(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 12)),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            retrospectiveId = null
        )

        every { taskRepository.saveAll(any()) } returns listOf(updatedTask)

        When("서비스를 실행하면") {
            val result = service.execute(command)

            Then("상태가 CANCEL로 변경되어야 한다") {
                result.status shouldBe TaskStatus.CANCEL
            }
        }
    }
})
