package xyz.robinjoon.growweek.task.domain.model.command

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 완료된 회고 기간 관련 Command
 */
sealed interface CompletedRetrospectivePeriodCommand {
    /**
     * 완료된 회고 기간 저장 커맨드
     */
    data class Save(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val completedAt: LocalDateTime = LocalDateTime.now(),
    ) : CompletedRetrospectivePeriodCommand

    /**
     * 완료된 회고 기간 삭제 커맨드 (회고 삭제 시)
     */
    data class Delete(
        val retrospectiveId: RetrospectiveId,
    ) : CompletedRetrospectivePeriodCommand
}
