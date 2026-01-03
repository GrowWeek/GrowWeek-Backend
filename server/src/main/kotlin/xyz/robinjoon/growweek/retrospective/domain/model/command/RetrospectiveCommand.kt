package xyz.robinjoon.growweek.retrospective.domain.model.command

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.retrospective.domain.model.AdditionalNotes
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionCount
import xyz.robinjoon.growweek.retrospective.domain.model.QuestionId
import xyz.robinjoon.growweek.retrospective.domain.model.RetrospectivePeriod

sealed interface RetrospectiveCommand {
    /**
     * 회고 생성 커맨드
     */
    data class CreateRetrospective(
        val memberId: MemberId,
        val period: RetrospectivePeriod,
        val questionCount: QuestionCount = QuestionCount.DEFAULT
    ) : RetrospectiveCommand

    /**
     * 질문 생성 커맨드
     */
    data class GenerateQuestions(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId
    ) : RetrospectiveCommand

    /**
     * 질문 생성 완료 커맨드
     */
    data class CompleteQuestionGeneration(
        val retrospectiveId: RetrospectiveId,
        val generatedQuestionContents: List<String>
    ) : RetrospectiveCommand

    /**
     * 답변 작성/수정 커맨드
     */
    data class WriteAnswer(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
        val questionId: QuestionId,
        val content: String?
    ) : RetrospectiveCommand

    /**
     * 기타 회고 내용 작성 커맨드
     */
    data class WriteAdditionalNotes(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
        val notes: AdditionalNotes
    ) : RetrospectiveCommand

    /**
     * 회고 완료 커맨드
     */
    data class CompleteRetrospective(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId
    ) : RetrospectiveCommand

    /**
     * 회고 삭제 커맨드
     */
    data class DeleteRetrospective(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId
    ) : RetrospectiveCommand
}
