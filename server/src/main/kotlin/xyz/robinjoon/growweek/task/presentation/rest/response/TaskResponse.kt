package xyz.robinjoon.growweek.task.presentation.rest.response

import xyz.robinjoon.growweek.task.application.dto.TaskDto
import java.time.format.DateTimeFormatter

/**
 * 할일 응답 DTO
 */
data class TaskResponse(
    /** 할일 고유 식별자 */
    val id: Long,

    /** 할일 제목 */
    val title: String,

    /** 할일 설명 */
    val description: String?,

    /** 할일 상태 (TODO, IN_PROGRESS, DONE, CANCEL) */
    val status: String,

    /** 민감도 수준 (NONE, TITLE_ONLY, NEVER) */
    val sensitivityLevel: String,

    /** 할일 우선순위 (1 이상의 정수, 값이 작을수록 높은 우선순위) */
    val priority: Int,

    /** 할일 시작 날짜 (yyyy-MM-dd) */
    val startDate: String,

    /** 할일 마감 날짜 (yyyy-MM-dd) */
    val dueDate: String,

    /** 회고 작성 여부 */
    val hasRetrospective: Boolean,

    /** 생성 일시 (yyyy-MM-ddTHH:mm:ss) */
    val createdAt: String,

    /** 수정 일시 (yyyy-MM-ddTHH:mm:ss) */
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
