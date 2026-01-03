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

class CreateTaskServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val taskRepository = mockk<TaskRepository>()
    val service = CreateTaskService(taskRepository)

    Given("할일 생성 요청이 왔을 때") {
        val memberId = MemberId(1L)
        val command = TaskApplicationCommand.CreateTask(
            memberId = memberId,
            title = "테스트 할일",
            description = "테스트 설명",
            priority = 1,
            startDate = LocalDate.of(2025, 1, 6),
            dueDate = LocalDate.of(2025, 1, 12),
            sensitivityLevel = SensitivityLevel.NONE
        )

        val savedTask = createTask(
            title = command.title,
            description = command.description,
            priority = command.priority,
            startDate = command.startDate,
            dueDate = command.dueDate,
            sensitivityLevel = command.sensitivityLevel
        )

        val commandSlot = slot<List<TaskCommand>>()
        every { taskRepository.saveAll(capture(commandSlot)) } returns listOf(savedTask)

        When("서비스를 실행하면") {
            val result = service.execute(command)

            Then("Repository에 저장 요청을 해야 한다") {
                verify(exactly = 1) { taskRepository.saveAll(any()) }
            }

            Then("Application Command가 Domain Command로 변환되어야 한다") {
                val capturedCommand = commandSlot.captured.first()
                capturedCommand shouldBe TaskCommand.CreateTask(
                    memberId = memberId,
                    title = TaskTitle(command.title),
                    description = TaskDescription(command.description!!),
                    priority = Priority(command.priority),
                    period = TaskPeriod(command.startDate, command.dueDate),
                    sensitivityLevel = command.sensitivityLevel
                )
            }

            Then("생성된 Task의 DTO를 반환해야 한다") {
                result.title.value shouldBe command.title
                result.description?.value shouldBe command.description
                result.priority.value shouldBe command.priority
                result.startDate shouldBe command.startDate
                result.dueDate shouldBe command.dueDate
                result.sensitivityLevel shouldBe command.sensitivityLevel
                result.status shouldBe TaskStatus.TODO
            }
        }
    }

    Given("설명 없이 할일 생성 요청이 왔을 때") {
        val command = TaskApplicationCommand.CreateTask(
            memberId = MemberId(1L),
            title = "설명 없는 할일",
            description = null,
            priority = 2,
            startDate = LocalDate.of(2025, 1, 6),
            dueDate = LocalDate.of(2025, 1, 10),
            sensitivityLevel = SensitivityLevel.TITLE_ONLY
        )

        val savedTask = createTask(
            title = command.title,
            description = null,
            priority = command.priority,
            startDate = command.startDate,
            dueDate = command.dueDate,
            sensitivityLevel = command.sensitivityLevel
        )

        every { taskRepository.saveAll(any()) } returns listOf(savedTask)

        When("서비스를 실행하면") {
            val result = service.execute(command)

            Then("설명이 null인 Task가 생성되어야 한다") {
                result.description shouldBe null
            }

            Then("민감도가 TITLE_ONLY로 설정되어야 한다") {
                result.sensitivityLevel shouldBe SensitivityLevel.TITLE_ONLY
            }
        }
    }
})

private fun createTask(
    title: String,
    description: String?,
    priority: Int,
    startDate: LocalDate,
    dueDate: LocalDate,
    sensitivityLevel: SensitivityLevel
): Task {
    val now = LocalDateTime.now()
    return Task(
        id = TaskId(1L),
        memberId = MemberId(1L),
        title = TaskTitle(title),
        description = description?.let { TaskDescription(it) },
        status = TaskStatus.TODO,
        sensitivityLevel = sensitivityLevel,
        priority = Priority(priority),
        period = TaskPeriod(startDate, dueDate),
        createdAt = now,
        updatedAt = now,
        retrospectiveId = null
    )
}
