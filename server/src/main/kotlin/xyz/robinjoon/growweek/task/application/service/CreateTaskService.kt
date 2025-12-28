package xyz.robinjoon.growweek.task.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand
import xyz.robinjoon.growweek.task.application.dto.TaskDto
import xyz.robinjoon.growweek.task.application.usecase.CreateTaskUseCase
import xyz.robinjoon.growweek.task.domain.model.*
import xyz.robinjoon.growweek.task.domain.model.command.TaskCommand
import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

@Service
class CreateTaskService(
    private val taskRepository: TaskRepository
) : CreateTaskUseCase {

    @Transactional
    override fun execute(command: TaskApplicationCommand.CreateTask): TaskDto {
        // Application Command를 Domain Command로 변환
        val domainCommand = TaskCommand.CreateTask(
            memberId = command.memberId,
            title = TaskTitle(command.title),
            description = command.description?.let { TaskDescription(it) },
            priority = Priority(command.priority),
            period = TaskPeriod(command.startDate, command.dueDate),
            sensitivityLevel = command.sensitivityLevel
        )

        // Repository를 통해 저장
        val savedTasks = taskRepository.saveAll(listOf(domainCommand))
        val createdTask = savedTasks.first()

        return TaskDto.from(createdTask)
    }
}
