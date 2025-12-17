package xyz.robinjoon.growweek.task.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand
import xyz.robinjoon.growweek.task.application.dto.TaskDto
import xyz.robinjoon.growweek.task.application.usecase.UpdateTaskStatusUseCase
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

@Service
class UpdateTaskStatusService(
    private val taskRepository: TaskRepository
) : UpdateTaskStatusUseCase {

    @Transactional
    override fun execute(command: TaskApplicationCommand.UpdateTaskStatus): TaskDto {
        // Application Command를 Domain Command로 변환
        val domainCommand = TaskCommand.UpdateTaskStatus(
            taskId = command.taskId,
            userId = command.userId,
            status = command.status
        )

        // Repository를 통해 업데이트
        val savedTasks = taskRepository.saveAll(listOf(domainCommand))
        val updatedTask = savedTasks.first()

        return TaskDto.from(updatedTask)
    }
}
