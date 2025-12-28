package xyz.robinjoon.growweek.task.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand
import xyz.robinjoon.growweek.task.application.usecase.DeleteTaskUseCase
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

@Service
class DeleteTaskService(
    private val taskRepository: TaskRepository
) : DeleteTaskUseCase {

    @Transactional
    override fun execute(command: TaskApplicationCommand.DeleteTask) {
        // Application Command를 Domain Command로 변환
        val domainCommand = TaskCommand.DeleteTask(
            taskId = command.taskId,
            memberId = command.memberId
        )

        // Repository를 통해 삭제
        taskRepository.saveAll(listOf(domainCommand))
    }
}
