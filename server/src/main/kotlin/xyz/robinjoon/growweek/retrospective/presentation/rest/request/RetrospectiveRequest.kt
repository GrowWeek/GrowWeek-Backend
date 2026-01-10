package xyz.robinjoon.growweek.retrospective.presentation.rest.request

/**
 * 회고 생성 요청 DTO
 */
data class CreateRetrospectiveRequest(
    /** 주 식별자 (YYYY-Www 형식, 예: 2025-W02) */
    val weekId: String,
    /** 생성할 질문 수 (기본값: 3) */
    val questionCount: Int = 3,
)

/**
 * 답변 작성 요청 DTO
 */
data class WriteAnswerRequest(
    /** 질문 식별자 */
    val questionId: Long,
    /** 답변 내용 (null이면 답변 삭제) */
    val content: String?,
)

/**
 * 추가 메모 작성 요청 DTO
 */
data class WriteAdditionalNotesRequest(
    /** 추가 메모 내용 */
    val notes: String,
)
