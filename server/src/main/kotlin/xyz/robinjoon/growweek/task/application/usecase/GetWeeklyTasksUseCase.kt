package xyz.robinjoon.growweek.task.application.usecase

import xyz.robinjoon.growweek.task.application.dto.WeeklyTaskDto
import xyz.robinjoon.growweek.task.application.query.TaskApplicationQuery

interface GetWeeklyTasksUseCase {
    fun execute(query: TaskApplicationQuery.OffsetByUserIdAndWeek): WeeklyTaskDto
    fun execute(query: TaskApplicationQuery.CursorByUserIdAndWeek): WeeklyTaskDto
}
