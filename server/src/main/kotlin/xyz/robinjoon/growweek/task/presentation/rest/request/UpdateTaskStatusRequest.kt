package xyz.robinjoon.growweek.task.presentation.rest.request

/**
 * 할일 상태 변경 요청 DTO
 */
data class UpdateTaskStatusRequest(
    /** 변경할 할일 상태 (TODO, IN_PROGRESS, DONE, CANCEL) */
    val status: String
)
