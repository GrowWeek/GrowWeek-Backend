package xyz.robinjoon.growweek.task.application.service

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

class DeleteTaskServiceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerLeaf

        val taskRepository = mockk<TaskRepository>()
        val service = DeleteTaskService(taskRepository)

        Given("할일 삭제 요청이 왔을 때") {
            val taskId = TaskId(1L)
            val memberId = MemberId(1L)

            val command =
                TaskApplicationCommand.DeleteTask(
                    taskId = taskId,
                    memberId = memberId,
                )

            val commandSlot = slot<List<TaskCommand>>()
            every { taskRepository.saveAll(capture(commandSlot)) } returns emptyList()

            When("서비스를 실행하면") {
                service.execute(command)

                Then("Repository에 삭제 요청을 해야 한다") {
                    verify(exactly = 1) { taskRepository.saveAll(any()) }
                }

                Then("Application Command가 Domain Command로 변환되어야 한다") {
                    val capturedCommand = commandSlot.captured.first() as TaskCommand.DeleteTask
                    capturedCommand.taskId shouldBe taskId
                    capturedCommand.memberId shouldBe memberId
                }
            }
        }
    })
