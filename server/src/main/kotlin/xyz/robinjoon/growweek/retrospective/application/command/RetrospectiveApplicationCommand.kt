package xyz.robinjoon.growweek.retrospective.application.command

import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.UserId
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionId
import java.time.LocalDate

sealed interface RetrospectiveApplicationCommand {

    data class CreateRetrospective(
        val userId: UserId,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val questionCount: Int = 3
    ) : RetrospectiveApplicationCommand

    data class GenerateQuestions(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId
    ) : RetrospectiveApplicationCommand

    data class WriteAnswer(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId,
        val questionId: QuestionId,
        val content: String?
    ) : RetrospectiveApplicationCommand

    data class WriteAdditionalNotes(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId,
        val notes: String
    ) : RetrospectiveApplicationCommand

    data class CompleteRetrospective(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId
    ) : RetrospectiveApplicationCommand

    data class DeleteRetrospective(
        val retrospectiveId: RetrospectiveId,
        val userId: UserId
    ) : RetrospectiveApplicationCommand
}
