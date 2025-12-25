package xyz.robinjoon.growweek.retrospective.presentation.rest.response

import xyz.robinjoon.growweek.retrospective.application.dto.*
import java.time.format.DateTimeFormatter

data class RetrospectiveResponse(
    val id: Long,
    val startDate: String,
    val endDate: String,
    val status: String,
    val questionCount: Int,
    val questions: List<QuestionResponse>,
    val answers: List<AnswerResponse>,
    val additionalNotes: String?,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(dto: RetrospectiveDto): RetrospectiveResponse {
            return RetrospectiveResponse(
                id = dto.id.value,
                startDate = dto.startDate.format(dateFormatter),
                endDate = dto.endDate.format(dateFormatter),
                status = dto.status.name,
                questionCount = dto.questionCount,
                questions = dto.questions.map { QuestionResponse.from(it) },
                answers = dto.answers.map { AnswerResponse.from(it) },
                additionalNotes = dto.additionalNotes,
                createdAt = dto.createdAt.format(dateTimeFormatter),
                updatedAt = dto.updatedAt.format(dateTimeFormatter)
            )
        }
    }
}

data class QuestionResponse(
    val id: Long,
    val content: String,
    val order: Int,
    val createdAt: String
) {
    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(dto: QuestionDto): QuestionResponse {
            return QuestionResponse(
                id = dto.id.value,
                content = dto.content,
                order = dto.order,
                createdAt = dto.createdAt.format(dateTimeFormatter)
            )
        }
    }
}

data class AnswerResponse(
    val id: Long?,
    val questionId: Long,
    val content: String?,
    val createdAt: String,
    val updatedAt: String
) {
    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(dto: AnswerDto): AnswerResponse {
            return AnswerResponse(
                id = dto.id?.value,
                questionId = dto.questionId.value,
                content = dto.content,
                createdAt = dto.createdAt.format(dateTimeFormatter),
                updatedAt = dto.updatedAt.format(dateTimeFormatter)
            )
        }
    }
}

data class RetrospectiveSummaryResponse(
    val id: Long,
    val startDate: String,
    val endDate: String,
    val status: String,
    val questionCount: Int,
    val answeredCount: Int,
    val createdAt: String
) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun from(dto: RetrospectiveSummaryDto): RetrospectiveSummaryResponse {
            return RetrospectiveSummaryResponse(
                id = dto.id.value,
                startDate = dto.startDate.format(dateFormatter),
                endDate = dto.endDate.format(dateFormatter),
                status = dto.status.name,
                questionCount = dto.questionCount,
                answeredCount = dto.answeredCount,
                createdAt = dto.createdAt.format(dateTimeFormatter)
            )
        }
    }
}

data class MonthlyRetrospectiveResponse(
    val year: Int,
    val month: Int,
    val retrospectives: List<RetrospectiveSummaryResponse>,
    val statistics: RetrospectiveStatisticsResponse
) {
    companion object {
        fun from(dto: MonthlyRetrospectiveDto): MonthlyRetrospectiveResponse {
            return MonthlyRetrospectiveResponse(
                year = dto.year,
                month = dto.month,
                retrospectives = dto.retrospectives.map { RetrospectiveSummaryResponse.from(it) },
                statistics = RetrospectiveStatisticsResponse.from(dto.statistics)
            )
        }
    }
}

data class RetrospectiveStatisticsResponse(
    val total: Int,
    val completed: Int,
    val inProgress: Int,
    val notStarted: Int
) {
    companion object {
        fun from(dto: RetrospectiveStatisticsDto): RetrospectiveStatisticsResponse {
            return RetrospectiveStatisticsResponse(
                total = dto.total,
                completed = dto.completed,
                inProgress = dto.inProgress,
                notStarted = dto.notStarted
            )
        }
    }
}
