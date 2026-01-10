package xyz.robinjoon.growweek.task.domain.model.command

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.WeekId
import java.time.LocalDateTime

/**
 * 완료된 회고 주 관련 Command
 */
sealed interface CompletedWeekCommand {
    /**
     * 완료된 회고 주 저장 커맨드
     */
    data class Save(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
        val weekId: WeekId,
        val completedAt: LocalDateTime = LocalDateTime.now(),
    ) : CompletedWeekCommand

    /**
     * 완료된 회고 주 삭제 커맨드 (회고 삭제 시)
     */
    data class Delete(
        val retrospectiveId: RetrospectiveId,
    ) : CompletedWeekCommand
}
