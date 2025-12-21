package xyz.robinjoon.growweek.retrospective.domain.model

import java.time.LocalDateTime

data class Answer(
    val id: AnswerId,
    val questionId: QuestionId,
    val content: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    fun updateContent(newContent: String?): Answer {
        return copy(
            content = newContent,
            updatedAt = LocalDateTime.now()
        )
    }
}
