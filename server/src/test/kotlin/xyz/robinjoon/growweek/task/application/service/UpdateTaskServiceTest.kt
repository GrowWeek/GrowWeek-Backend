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

class UpdateTaskServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val taskRepository = mockk<TaskRepository>()
    val service = UpdateTaskService(taskRepository)

    Given("할일 수정 요청이 왔을 때") {
        val taskId = TaskId(1L)
        val memberId = MemberId(1L)
        val command = TaskApplicationCommand.UpdateTask(
            taskId = taskId,
            memberId = memberId,
            title = "수정된 제목",
            description = "수정된 설명",
            status = TaskStatus.IN_PROGRESS,
            priority = 3,
            dueDate = LocalDate.of(2025, 1, 20),
            sensitivityLevel = SensitivityLevel.NEVER
        )

        val updatedTask = Task(
            id = taskId,
            memberId = memberId,
            title = TaskTitle(command.title!!),
            description = TaskDescription(command.description!!),
            status = command.status!!,
            sensitivityLevel = command.sensitivityLevel!!,
            priority = Priority(command.priority!!),
            period = TaskPeriod(LocalDate.of(2025, 1, 6), command.dueDate!!),
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
                val capturedCommand = commandSlot.captured.first() as TaskCommand.UpdateTask
                capturedCommand.taskId shouldBe taskId
                capturedCommand.memberId shouldBe memberId
                capturedCommand.title shouldBe TaskTitle(command.title!!)
                capturedCommand.description shouldBe TaskDescription(command.description!!)
                capturedCommand.status shouldBe command.status
                capturedCommand.priority shouldBe Priority(command.priority!!)
                capturedCommand.dueDate shouldBe command.dueDate
                capturedCommand.sensitivityLevel shouldBe command.sensitivityLevel
            }

            Then("수정된 Task의 DTO를 반환해야 한다") {
                result.id shouldBe taskId
                result.title.value shouldBe command.title
                result.description?.value shouldBe command.description
                result.status shouldBe command.status
                result.priority.value shouldBe command.priority
                result.dueDate shouldBe command.dueDate
                result.sensitivityLevel shouldBe command.sensitivityLevel
            }
        }
    }

    Given("일부 필드만 수정 요청이 왔을 때") {
        val taskId = TaskId(1L)
        val memberId = MemberId(1L)
        val command = TaskApplicationCommand.UpdateTask(
            taskId = taskId,
            memberId = memberId,
            title = "제목만 수정",
            description = null,
            status = null,
            priority = null,
            dueDate = null,
            sensitivityLevel = null
        )

        val updatedTask = Task(
            id = taskId,
            memberId = memberId,
            title = TaskTitle(command.title!!),
            description = TaskDescription("기존 설명"),
            status = TaskStatus.TODO,
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

            Then("null인 필드는 Domain Command에도 null로 전달되어야 한다") {
                val capturedCommand = commandSlot.captured.first() as TaskCommand.UpdateTask
                capturedCommand.title shouldBe TaskTitle(command.title!!)
                capturedCommand.description shouldBe null
                capturedCommand.status shouldBe null
                capturedCommand.priority shouldBe null
                capturedCommand.dueDate shouldBe null
                capturedCommand.sensitivityLevel shouldBe null
            }

            Then("수정된 Task의 DTO를 반환해야 한다") {
                result.title.value shouldBe command.title
            }
        }
    }
})
