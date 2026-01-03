package xyz.robinjoon.growweek.retrospective.application.dto

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.retrospective.domain.model.*
import java.time.LocalDate
import java.time.LocalDateTime

data class RetrospectiveDto(
    val id: RetrospectiveId,
    val memberId: MemberId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: RetrospectiveStatus,
    val questionCount: Int,
    val questions: List<QuestionDto>,
    val answers: List<AnswerDto>,
    val additionalNotes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(retrospective: Retrospective): RetrospectiveDto =
            RetrospectiveDto(
                id = retrospective.id,
                memberId = retrospective.memberId,
                startDate = retrospective.period.startDate,
                endDate = retrospective.period.endDate,
                status = retrospective.status,
                questionCount = retrospective.questionCount.value,
                questions = retrospective.questions.map { QuestionDto.from(it) },
                answers = retrospective.answers.values.map { AnswerDto.from(it) },
                additionalNotes = retrospective.additionalNotes?.value,
                createdAt = retrospective.createdAt,
                updatedAt = retrospective.updatedAt,
            )
    }
}

data class QuestionDto(
    val id: QuestionId,
    val retrospectiveId: RetrospectiveId,
    val content: String,
    val order: Int,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(question: Question): QuestionDto =
            QuestionDto(
                id = question.id,
                retrospectiveId = question.retrospectiveId,
                content = question.content,
                order = question.order,
                createdAt = question.createdAt,
            )
    }
}

data class AnswerDto(
    val id: AnswerId?,
    val questionId: QuestionId,
    val content: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(answer: Answer): AnswerDto =
            AnswerDto(
                id = answer.id,
                questionId = answer.questionId,
                content = answer.content,
                createdAt = answer.createdAt,
                updatedAt = answer.updatedAt,
            )
    }
}

data class RetrospectiveSummaryDto(
    val id: RetrospectiveId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: RetrospectiveStatus,
    val questionCount: Int,
    val answeredCount: Int,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(retrospective: Retrospective): RetrospectiveSummaryDto =
            RetrospectiveSummaryDto(
                id = retrospective.id,
                startDate = retrospective.period.startDate,
                endDate = retrospective.period.endDate,
                status = retrospective.status,
                questionCount = retrospective.questionCount.value,
                answeredCount = retrospective.answers.count { it.value.content != null },
                createdAt = retrospective.createdAt,
            )
    }
}

data class MonthlyRetrospectiveDto(
    val year: Int,
    val month: Int,
    val retrospectives: List<RetrospectiveSummaryDto>,
    val statistics: RetrospectiveStatisticsDto,
)

data class RetrospectiveStatisticsDto(
    val total: Int,
    val completed: Int,
    val inProgress: Int,
    val notStarted: Int,
)
