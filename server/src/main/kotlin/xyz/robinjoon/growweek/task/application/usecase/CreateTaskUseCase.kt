package xyz.robinjoon.growweek.task.application.usecase

import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand
import xyz.robinjoon.growweek.task.application.dto.TaskDto

interface CreateTaskUseCase {
    fun execute(command: TaskApplicationCommand.CreateTask): TaskDto
}
