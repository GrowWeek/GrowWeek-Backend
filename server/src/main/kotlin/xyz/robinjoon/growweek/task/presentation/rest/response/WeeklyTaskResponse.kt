package xyz.robinjoon.growweek.task.presentation.rest.response

import xyz.robinjoon.growweek.task.application.dto.WeeklyTaskDto
import java.time.format.DateTimeFormatter

/**
 * 주간 할일 응답 DTO
 */
data class WeeklyTaskResponse(
    /** 주 시작일 (yyyy-MM-dd) */
    val weekStart: String,

    /** 주 종료일 (yyyy-MM-dd) */
    val weekEnd: String,

    /** 해당 주의 할일 목록 */
    val tasks: List<TaskResponse>,

    /** 해당 주의 할일 통계 */
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

/**
 * 할일 통계 응답 DTO
 */
data class TaskStatisticsResponse(
    /** 전체 할일 수 */
    val total: Int,

    /** 할 일 상태 수 */
    val todo: Int,

    /** 진행 중 상태 수 */
    val inProgress: Int,

    /** 완료 상태 수 */
    val done: Int,

    /** 취소 상태 수 */
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
