package xyz.robinjoon.growweek.task.application.usecase

import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand
import xyz.robinjoon.growweek.task.application.dto.TaskDto

interface UpdateTaskStatusUseCase {
    fun execute(command: TaskApplicationCommand.UpdateTaskStatus): TaskDto
}
