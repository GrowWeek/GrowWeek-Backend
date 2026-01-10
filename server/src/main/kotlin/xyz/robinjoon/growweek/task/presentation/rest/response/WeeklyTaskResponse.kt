package xyz.robinjoon.growweek.task.presentation.rest.response

import xyz.robinjoon.growweek.task.application.dto.WeeklyTaskDto

/**
 * 주간 할일 응답 DTO
 */
data class WeeklyTaskResponse(
    /** 주 식별자 (YYYY-Www 형식, 예: 2025-W02) */
    val weekId: String,
    /** 해당 주의 할일 목록 */
    val tasks: List<TaskResponse>,
    /** 해당 주의 할일 통계 */
    val statistics: TaskStatisticsResponse,
) {
    companion object {
        fun from(dto: WeeklyTaskDto): WeeklyTaskResponse =
            WeeklyTaskResponse(
                weekId = dto.weekId.value,
                tasks = dto.tasks.map { TaskResponse.from(it) },
                statistics = TaskStatisticsResponse.from(dto.statistics),
            )
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
    val cancel: Int,
) {
    companion object {
        fun from(dto: xyz.robinjoon.growweek.task.application.dto.TaskStatisticsDto): TaskStatisticsResponse =
            TaskStatisticsResponse(
                total = dto.total,
                todo = dto.todo,
                inProgress = dto.inProgress,
                done = dto.done,
                cancel = dto.cancel,
            )
    }
}
