package xyz.robinjoon.growweek.retrospective.application.command

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.WeekId
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionId

sealed interface RetrospectiveApplicationCommand {
    data class CreateRetrospective(
        val memberId: MemberId,
        val weekId: WeekId,
        val questionCount: Int = 3,
    ) : RetrospectiveApplicationCommand

    data class GenerateQuestions(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
    ) : RetrospectiveApplicationCommand

    data class WriteAnswer(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
        val questionId: QuestionId,
        val content: String?,
    ) : RetrospectiveApplicationCommand

    data class WriteAdditionalNotes(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
        val notes: String,
    ) : RetrospectiveApplicationCommand

    data class CompleteRetrospective(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
    ) : RetrospectiveApplicationCommand

    data class DeleteRetrospective(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
    ) : RetrospectiveApplicationCommand

    companion object {
        fun createRetrospective(
            memberId: Long,
            weekId: String,
            questionCount: Int = 3,
        ): CreateRetrospective =
            CreateRetrospective(
                memberId = MemberId(memberId),
                weekId = WeekId(weekId),
                questionCount = questionCount,
            )

        fun generateQuestions(
            retrospectiveId: Long,
            memberId: Long,
        ): GenerateQuestions =
            GenerateQuestions(
                retrospectiveId = RetrospectiveId(retrospectiveId),
                memberId = MemberId(memberId),
            )

        fun writeAnswer(
            retrospectiveId: Long,
            memberId: Long,
            questionId: Long,
            content: String?,
        ): WriteAnswer =
            WriteAnswer(
                retrospectiveId = RetrospectiveId(retrospectiveId),
                memberId = MemberId(memberId),
                questionId = QuestionId(questionId),
                content = content,
            )

        fun writeAdditionalNotes(
            retrospectiveId: Long,
            memberId: Long,
            notes: String,
        ): WriteAdditionalNotes =
            WriteAdditionalNotes(
                retrospectiveId = RetrospectiveId(retrospectiveId),
                memberId = MemberId(memberId),
                notes = notes,
            )

        fun completeRetrospective(
            retrospectiveId: Long,
            memberId: Long,
        ): CompleteRetrospective =
            CompleteRetrospective(
                retrospectiveId = RetrospectiveId(retrospectiveId),
                memberId = MemberId(memberId),
            )

        fun deleteRetrospective(
            retrospectiveId: Long,
            memberId: Long,
        ): DeleteRetrospective =
            DeleteRetrospective(
                retrospectiveId = RetrospectiveId(retrospectiveId),
                memberId = MemberId(memberId),
            )
    }
}
