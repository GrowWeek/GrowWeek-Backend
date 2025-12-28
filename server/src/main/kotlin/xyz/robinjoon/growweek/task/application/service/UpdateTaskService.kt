package xyz.robinjoon.growweek.task.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand
import xyz.robinjoon.growweek.task.application.dto.TaskDto
import xyz.robinjoon.growweek.task.application.usecase.UpdateTaskUseCase
import xyz.robinjoon.growweek.task.domain.model.*
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

@Service
class UpdateTaskService(
    private val taskRepository: TaskRepository
) : UpdateTaskUseCase {

    @Transactional
    override fun execute(command: TaskApplicationCommand.UpdateTask): TaskDto {
        // Application Command를 Domain Command로 변환
        val domainCommand = TaskCommand.UpdateTask(
            taskId = command.taskId,
            memberId = command.memberId,
            title = command.title?.let { TaskTitle(it) },
            description = command.description?.let { TaskDescription(it) },
            status = command.status,
            priority = command.priority?.let { Priority(it) },
            dueDate = command.dueDate,
            sensitivityLevel = command.sensitivityLevel
        )

        // Repository를 통해 업데이트
        val savedTasks = taskRepository.saveAll(listOf(domainCommand))
        val updatedTask = savedTasks.first()

        return TaskDto.from(updatedTask)
    }
}
