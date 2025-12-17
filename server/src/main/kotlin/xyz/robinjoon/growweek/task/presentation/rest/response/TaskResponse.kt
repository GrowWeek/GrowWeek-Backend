package xyz.robinjoon.growweek.task.presentation.rest.response

import xyz.robinjoon.growweek.task.application.dto.TaskDto
import java.time.format.DateTimeFormatter

data class TaskResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: String,
    val sensitivityLevel: String,
    val priority: Int,
    val startDate: String,
    val dueDate: String,
    val hasRetrospective: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(dto: TaskDto): TaskResponse {
            return TaskResponse(
                id = dto.id.value,
                title = dto.title.value,
                description = dto.description?.value,
                status = dto.status.name,
                sensitivityLevel = dto.sensitivityLevel.name,
                priority = dto.priority.value,
                startDate = dto.startDate.format(dateFormatter),
                dueDate = dto.dueDate.format(dateFormatter),
                hasRetrospective = dto.hasRetrospective,
                createdAt = dto.createdAt.format(dateTimeFormatter),
                updatedAt = dto.updatedAt.format(dateTimeFormatter)
            )
        }
    }
}
