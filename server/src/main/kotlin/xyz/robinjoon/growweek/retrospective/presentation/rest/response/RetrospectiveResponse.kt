package xyz.robinjoon.growweek.retrospective.presentation.rest.response

import xyz.robinjoon.growweek.retrospective.application.dto.*
import java.time.format.DateTimeFormatter

/**
 * 회고 상세 응답 DTO
 */
data class RetrospectiveResponse(
    /** 회고 고유 식별자 */
    val id: Long,
    /** 회고 시작일 (yyyy-MM-dd) */
    val startDate: String,
    /** 회고 종료일 (yyyy-MM-dd) */
    val endDate: String,
    /** 회고 상태 (TODO, BEFORE_GENERATE_QUESTION, AFTER_GENERATE_QUESTION, IN_PROGRESS, DONE) */
    val status: String,
    /** 질문 수 */
    val questionCount: Int,
    /** 질문 목록 */
    val questions: List<QuestionResponse>,
    /** 답변 목록 */
    val answers: List<AnswerResponse>,
    /** 추가 메모 */
    val additionalNotes: String?,
    /** 생성 일시 (yyyy-MM-ddTHH:mm:ss) */
    val createdAt: String,
    /** 수정 일시 (yyyy-MM-ddTHH:mm:ss) */
    val updatedAt: String,
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(dto: RetrospectiveDto): RetrospectiveResponse =
            RetrospectiveResponse(
                id = dto.id.value,
                startDate = dto.startDate.format(dateFormatter),
                endDate = dto.endDate.format(dateFormatter),
                status = dto.status.name,
                questionCount = dto.questionCount,
                questions = dto.questions.map { QuestionResponse.from(it) },
                answers = dto.answers.map { AnswerResponse.from(it) },
                additionalNotes = dto.additionalNotes,
                createdAt = dto.createdAt.format(dateTimeFormatter),
                updatedAt = dto.updatedAt.format(dateTimeFormatter),
            )
    }
}

/**
 * 질문 응답 DTO
 */
data class QuestionResponse(
    /** 질문 고유 식별자 */
    val id: Long,
    /** 질문 내용 */
    val content: String,
    /** 질문 순서 */
    val order: Int,
    /** 생성 일시 (yyyy-MM-ddTHH:mm:ss) */
    val createdAt: String,
) {
    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(dto: QuestionDto): QuestionResponse =
            QuestionResponse(
                id = dto.id.value,
                content = dto.content,
                order = dto.order,
                createdAt = dto.createdAt.format(dateTimeFormatter),
            )
    }
}

/**
 * 답변 응답 DTO
 */
data class AnswerResponse(
    /** 답변 고유 식별자 (아직 답변하지 않은 경우 null) */
    val id: Long?,
    /** 질문 식별자 */
    val questionId: Long,
    /** 답변 내용 */
    val content: String?,
    /** 생성 일시 (yyyy-MM-ddTHH:mm:ss) */
    val createdAt: String,
    /** 수정 일시 (yyyy-MM-ddTHH:mm:ss) */
    val updatedAt: String,
) {
    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(dto: AnswerDto): AnswerResponse =
            AnswerResponse(
                id = dto.id?.value,
                questionId = dto.questionId.value,
                content = dto.content,
                createdAt = dto.createdAt.format(dateTimeFormatter),
                updatedAt = dto.updatedAt.format(dateTimeFormatter),
            )
    }
}

/**
 * 회고 요약 응답 DTO
 */
data class RetrospectiveSummaryResponse(
    /** 회고 고유 식별자 */
    val id: Long,
    /** 회고 시작일 (yyyy-MM-dd) */
    val startDate: String,
    /** 회고 종료일 (yyyy-MM-dd) */
    val endDate: String,
    /** 회고 상태 (TODO, BEFORE_GENERATE_QUESTION, AFTER_GENERATE_QUESTION, IN_PROGRESS, DONE) */
    val status: String,
    /** 질문 수 */
    val questionCount: Int,
    /** 답변 완료된 질문 수 */
    val answeredCount: Int,
    /** 생성 일시 (yyyy-MM-ddTHH:mm:ss) */
    val createdAt: String,
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(dto: RetrospectiveSummaryDto): RetrospectiveSummaryResponse =
            RetrospectiveSummaryResponse(
                id = dto.id.value,
                startDate = dto.startDate.format(dateFormatter),
                endDate = dto.endDate.format(dateFormatter),
                status = dto.status.name,
                questionCount = dto.questionCount,
                answeredCount = dto.answeredCount,
                createdAt = dto.createdAt.format(dateTimeFormatter),
            )
    }
}

/**
 * 월간 회고 응답 DTO
 */
data class MonthlyRetrospectiveResponse(
    /** 년도 */
    val year: Int,
    /** 월 (1-12) */
    val month: Int,
    /** 해당 월의 회고 목록 */
    val retrospectives: List<RetrospectiveSummaryResponse>,
    /** 해당 월의 회고 통계 */
    val statistics: RetrospectiveStatisticsResponse,
) {
    companion object {
        fun from(dto: MonthlyRetrospectiveDto): MonthlyRetrospectiveResponse =
            MonthlyRetrospectiveResponse(
                year = dto.year,
                month = dto.month,
                retrospectives = dto.retrospectives.map { RetrospectiveSummaryResponse.from(it) },
                statistics = RetrospectiveStatisticsResponse.from(dto.statistics),
            )
    }
}

/**
 * 회고 통계 응답 DTO
 */
data class RetrospectiveStatisticsResponse(
    /** 전체 회고 수 */
    val total: Int,
    /** 완료된 회고 수 */
    val completed: Int,
    /** 진행 중인 회고 수 */
    val inProgress: Int,
    /** 시작하지 않은 회고 수 */
    val notStarted: Int,
) {
    companion object {
        fun from(dto: RetrospectiveStatisticsDto): RetrospectiveStatisticsResponse =
            RetrospectiveStatisticsResponse(
                total = dto.total,
                completed = dto.completed,
                inProgress = dto.inProgress,
                notStarted = dto.notStarted,
            )
    }
}
