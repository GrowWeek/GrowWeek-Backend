package xyz.robinjoon.growweek.task.presentation.rest.response

import xyz.robinjoon.growweek.task.application.dto.WeeklyTaskDto
import java.time.format.DateTimeFormatter

data class WeeklyTaskResponse(
    val weekStart: String,
    val weekEnd: String,
    val tasks: List<TaskResponse>,
    val statistics: TaskStatisticsResponse
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun from(dto: WeeklyTaskDto): WeeklyTaskResponse {
            return WeeklyTaskResponse(
                weekStart = dto.weekStart.format(dateFormatter),
                weekEnd = dto.weekEnd.format(dateFormatter),
                tasks = dto.tasks.map { TaskResponse.from(it) },
                statistics = TaskStatisticsResponse.from(dto.statistics)
            )
        }
    }
}

data class TaskStatisticsResponse(
    val total: Int,
    val todo: Int,
    val inProgress: Int,
    val done: Int,
    val cancel: Int
) {
    companion object {
        fun from(dto: xyz.robinjoon.growweek.task.application.dto.TaskStatisticsDto): TaskStatisticsResponse {
            return TaskStatisticsResponse(
                total = dto.total,
                todo = dto.todo,
                inProgress = dto.inProgress,
                done = dto.done,
                cancel = dto.cancel
            )
        }
    }
}
