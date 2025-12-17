package xyz.robinjoon.growweek.task.application.usecase

import xyz.robinjoon.growweek.task.application.command.TaskApplicationCommand

interface DeleteTaskUseCase {
    fun execute(command: TaskApplicationCommand.DeleteTask)
}
