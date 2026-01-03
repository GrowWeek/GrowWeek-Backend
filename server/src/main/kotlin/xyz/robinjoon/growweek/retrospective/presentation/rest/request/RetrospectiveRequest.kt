package xyz.robinjoon.growweek.retrospective.presentation.rest.request

/**
 * 회고 생성 요청 DTO
 */
data class CreateRetrospectiveRequest(
    /** 회고 시작일 (yyyy-MM-dd) */
    val startDate: String,
    /** 회고 종료일 (yyyy-MM-dd) */
    val endDate: String,
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
