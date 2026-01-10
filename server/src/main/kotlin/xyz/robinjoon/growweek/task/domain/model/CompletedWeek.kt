package xyz.robinjoon.growweek.task.domain.model

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.WeekId
import java.time.LocalDateTime

/**
 * 완료된 회고 주 정보
 *
 * 회고가 완료되면 해당 주 정보를 Task BC 내에 저장합니다.
 * Task 생성/수정 시 이 정보를 조회하여 회고 완료된 주에 대한 검증에 사용합니다.
 *
 * 이 도메인은 Retrospective BC를 직접 참조하지 않고,
 * 도메인 이벤트를 통해 전달받은 정보를 저장합니다.
 */
data class CompletedWeek(
    val retrospectiveId: RetrospectiveId,
    val memberId: MemberId,
    val weekId: WeekId,
    val completedAt: LocalDateTime,
)
