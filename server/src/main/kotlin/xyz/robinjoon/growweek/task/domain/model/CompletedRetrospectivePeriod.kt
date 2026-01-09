package xyz.robinjoon.growweek.task.domain.model

import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 완료된 회고 기간 정보
 *
 * 회고가 완료되면 해당 기간 정보를 Task BC 내에 저장합니다.
 * Task 생성/수정 시 이 정보를 조회하여 회고 완료된 기간에 대한 검증에 사용합니다.
 *
 * 이 도메인은 Retrospective BC를 직접 참조하지 않고,
 * 도메인 이벤트를 통해 전달받은 정보를 저장합니다.
 */
data class CompletedRetrospectivePeriod(
    val retrospectiveId: RetrospectiveId,
    val memberId: MemberId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val completedAt: LocalDateTime,
) {
    /**
     * 주어진 기간이 이 회고 기간과 겹치는지 확인
     */
    fun overlaps(
        periodStart: LocalDate,
        periodEnd: LocalDate,
    ): Boolean = !periodEnd.isBefore(startDate) && !periodStart.isAfter(endDate)

    /**
     * 주어진 날짜가 이 회고 기간에 포함되는지 확인
     */
    fun contains(date: LocalDate): Boolean = !date.isBefore(startDate) && !date.isAfter(endDate)
}
