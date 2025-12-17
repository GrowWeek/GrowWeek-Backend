package xyz.robinjoon.growweek.task.application.usecase

import xyz.robinjoon.growweek.common.Page
import xyz.robinjoon.growweek.task.application.dto.TaskDto
import xyz.robinjoon.growweek.task.application.query.TaskApplicationQuery

interface GetUserTasksUseCase {
    fun execute(query: TaskApplicationQuery): Page<TaskDto>
}
