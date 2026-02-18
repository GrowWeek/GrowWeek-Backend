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
    ) : RetrospectiveApplicationCommand {
        constructor(
            memberId: Long,
            weekId: String,
            questionCount: Int = 3,
            @Suppress("UNUSED_PARAMETER") marker: Unit = Unit,
        ) : this(
            memberId = MemberId(memberId),
            weekId = WeekId(weekId),
            questionCount = questionCount,
        )
    }

    data class GenerateQuestions(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
    ) : RetrospectiveApplicationCommand {
        constructor(
            retrospectiveId: Long,
            memberId: Long,
            @Suppress("UNUSED_PARAMETER") marker: Unit = Unit,
        ) : this(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            memberId = MemberId(memberId),
        )
    }

    data class WriteAnswer(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
        val questionId: QuestionId,
        val content: String?,
    ) : RetrospectiveApplicationCommand {
        constructor(
            retrospectiveId: Long,
            memberId: Long,
            questionId: Long,
            content: String?,
            @Suppress("UNUSED_PARAMETER") marker: Unit = Unit,
        ) : this(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            memberId = MemberId(memberId),
            questionId = QuestionId(questionId),
            content = content,
        )
    }

    data class WriteAdditionalNotes(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
        val notes: String,
    ) : RetrospectiveApplicationCommand {
        constructor(
            retrospectiveId: Long,
            memberId: Long,
            notes: String,
            @Suppress("UNUSED_PARAMETER") marker: Unit = Unit,
        ) : this(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            memberId = MemberId(memberId),
            notes = notes,
        )
    }

    data class CompleteRetrospective(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
    ) : RetrospectiveApplicationCommand {
        constructor(
            retrospectiveId: Long,
            memberId: Long,
            @Suppress("UNUSED_PARAMETER") marker: Unit = Unit,
        ) : this(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            memberId = MemberId(memberId),
        )
    }

    data class DeleteRetrospective(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
    ) : RetrospectiveApplicationCommand {
        constructor(
            retrospectiveId: Long,
            memberId: Long,
            @Suppress("UNUSED_PARAMETER") marker: Unit = Unit,
        ) : this(
            retrospectiveId = RetrospectiveId(retrospectiveId),
            memberId = MemberId(memberId),
        )
    }
}
