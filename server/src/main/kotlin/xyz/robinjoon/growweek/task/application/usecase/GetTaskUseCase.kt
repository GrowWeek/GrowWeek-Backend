package xyz.robinjoon.growweek.task.application.usecase

import xyz.robinjoon.growweek.task.application.dto.TaskDto
import xyz.robinjoon.growweek.task.application.query.TaskApplicationQuery

interface GetTaskUseCase {
    fun execute(query: TaskApplicationQuery): TaskDto?
}
