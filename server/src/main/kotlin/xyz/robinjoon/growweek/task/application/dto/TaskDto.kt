package xyz.robinjoon.growweek.task.application.dto

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.SensitivityLevel
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.task.domain.model.Priority
import xyz.robinjoon.growweek.task.domain.model.Task
import xyz.robinjoon.growweek.task.domain.model.TaskDescription
import xyz.robinjoon.growweek.task.domain.model.TaskStatus
import xyz.robinjoon.growweek.task.domain.model.TaskTitle
import java.time.LocalDate
import java.time.LocalDateTime

data class TaskDto(
    val id: TaskId,
    val memberId: MemberId,
    val title: TaskTitle,
    val description: TaskDescription?,
    val status: TaskStatus,
    val sensitivityLevel: SensitivityLevel,
    val priority: Priority,
    val weekId: WeekId,
    val dueDate: LocalDate,
    val hasRetrospective: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(task: Task): TaskDto =
            TaskDto(
                id = task.id,
                memberId = task.memberId,
                title = task.title,
                description = task.description,
                status = task.status,
                sensitivityLevel = task.sensitivityLevel,
                priority = task.priority,
                weekId = task.weekId,
                dueDate = task.dueDate,
                hasRetrospective = task.retrospectiveId != null,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
            )
    }
}

data class WeeklyTaskDto(
    val weekId: WeekId,
    val tasks: List<TaskDto>,
    val statistics: TaskStatisticsDto,
)

data class TaskStatisticsDto(
    val total: Int,
    val todo: Int,
    val inProgress: Int,
    val done: Int,
    val cancel: Int,
)
