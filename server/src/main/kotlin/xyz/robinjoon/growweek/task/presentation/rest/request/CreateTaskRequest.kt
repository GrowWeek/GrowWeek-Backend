package xyz.robinjoon.growweek.task.presentation.rest.request

/**
 * 할일 생성 요청 DTO
 */
data class CreateTaskRequest(
    /** 할일 제목 */
    val title: String,
    /** 할일 설명 (선택 사항) */
    val description: String?,
    /** 할일 우선순위 (1 이상의 정수, 값이 작을수록 높은 우선순위) */
    val priority: Int,
    /** 할일 시작 날짜 (yyyy-MM-dd) */
    val startDate: String,
    /** 할일 마감 날짜 (yyyy-MM-dd) */
    val dueDate: String,
    /** 민감도 수준 (NONE, TITLE_ONLY, NEVER) */
    val sensitivityLevel: String? = "NONE",
)
