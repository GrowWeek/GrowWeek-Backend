package xyz.robinjoon.growweek.retrospective.domain.model

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.WeekId
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

data class Retrospective(
    val id: RetrospectiveId,
    val memberId: MemberId,
    val weekId: WeekId,
    val status: RetrospectiveStatus,
    val questionCount: QuestionCount,
    val questions: List<Question>,
    val answers: Map<QuestionId, Answer>,
    val additionalNotes: AdditionalNotes?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    /**
     * 회고 작성 가능 여부 확인
     * 종료일 2일 전(금요일)부터 다음 주 월요일 0시 전까지 작성 가능
     */
    fun canWrite(currentDate: LocalDate = LocalDate.now()): Boolean {
        if (status == RetrospectiveStatus.DONE) return false

        val writableStartDate = weekId.endDate.minusDays(DAYS_BEFORE_END_DATE_TO_START_WRITING)
        val writableEndDate = calculateNextMonday(weekId.endDate)
        return !currentDate.isBefore(writableStartDate) && currentDate.isBefore(writableEndDate)
    }

    private fun calculateNextMonday(date: LocalDate): LocalDate {
        val daysUntilMonday = (DayOfWeek.MONDAY.value - date.dayOfWeek.value + 7) % 7
        return if (daysUntilMonday == 0) {
            date.plusDays(7)
        } else {
            date.plusDays(daysUntilMonday.toLong())
        }
    }

    /**
     * 질문 생성 시작
     */
    fun startGeneratingQuestions(): Retrospective {
        require(status == RetrospectiveStatus.TODO) {
            "질문 생성은 TODO 상태에서만 가능합니다"
        }
        return copy(
            status = RetrospectiveStatus.BEFORE_GENERATE_QUESTION,
            updatedAt = LocalDateTime.now(),
        )
    }

    /**
     * 질문 생성 완료
     */
    fun completeQuestionGeneration(generatedQuestions: List<Question>): Retrospective {
        require(status == RetrospectiveStatus.BEFORE_GENERATE_QUESTION) {
            "질문 생성 완료는 BEFORE_GENERATE_QUESTION 상태에서만 가능합니다"
        }
        require(generatedQuestions.size == questionCount.value) {
            "생성된 질문 개수가 설정된 개수와 일치하지 않습니다"
        }
        return copy(
            status = RetrospectiveStatus.AFTER_GENERATE_QUESTION,
            questions = generatedQuestions,
            updatedAt = LocalDateTime.now(),
        )
    }

    /**
     * 답변 작성/수정
     */
    fun writeAnswer(
        questionId: QuestionId,
        content: String?,
    ): Retrospective {
        require(canWrite()) {
            "회고 작성 기간이 지났거나 이미 완료된 회고입니다"
        }
        require(questions.any { it.id == questionId }) {
            "존재하지 않는 질문입니다"
        }

        val existingAnswer = answers[questionId]
        val now = LocalDateTime.now()
        val newAnswer =
            Answer(
                id = existingAnswer?.id,
                questionId = questionId,
                content = content,
                createdAt = existingAnswer?.createdAt ?: now,
                updatedAt = now,
            )

        val newStatus =
            if (status == RetrospectiveStatus.AFTER_GENERATE_QUESTION) {
                RetrospectiveStatus.IN_PROGRESS
            } else {
                status
            }

        return copy(
            status = newStatus,
            answers = answers + (questionId to newAnswer),
            updatedAt = now,
        )
    }

    /**
     * 기타 회고 내용 작성
     */
    fun writeAdditionalNotes(notes: AdditionalNotes): Retrospective {
        require(canWrite()) {
            "회고 작성 기간이 지났거나 이미 완료된 회고입니다"
        }
        return copy(
            additionalNotes = notes,
            updatedAt = LocalDateTime.now(),
        )
    }

    /**
     * 회고 완료
     */
    fun complete(): Retrospective {
        require(status == RetrospectiveStatus.IN_PROGRESS) {
            "답변을 하나 이상 작성한 후 완료할 수 있습니다"
        }
        return copy(
            status = RetrospectiveStatus.DONE,
            updatedAt = LocalDateTime.now(),
        )
    }

    companion object {
        private const val DAYS_BEFORE_END_DATE_TO_START_WRITING = 2L
    }
}
