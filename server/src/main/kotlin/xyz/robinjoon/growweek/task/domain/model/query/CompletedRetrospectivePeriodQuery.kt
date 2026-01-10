package xyz.robinjoon.growweek.task.domain.model.query

import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import java.time.LocalDate

/**
 * 완료된 회고 기간 조회 Query
 */
sealed class CompletedRetrospectivePeriodQuery(
    override val pageInfo: PageInfo,
) : PageQuery {
    /**
     * Offset 기반 쿼리 팩토리
     */
    object Offset {
        /**
         * 특정 회원의 특정 기간과 겹치는 완료된 회고 조회
         */
        fun byMemberIdAndOverlappingPeriod(
            memberId: MemberId,
            periodStart: LocalDate,
            periodEnd: LocalDate,
            page: Int = 0,
            size: Int = 100,
        ): OffsetByMemberIdAndOverlappingPeriod =
            OffsetByMemberIdAndOverlappingPeriod(
                memberId = memberId,
                periodStart = periodStart,
                periodEnd = periodEnd,
                pageInfo =
                    OffsetPageInfo(
                        page = page,
                        size = size,
                        orderBy = "completedAt",
                    ),
            )

        /**
         * 특정 회고 ID로 조회
         */
        fun byRetrospectiveId(
            retrospectiveId: RetrospectiveId,
            page: Int = 0,
            size: Int = 1,
        ): OffsetByRetrospectiveId =
            OffsetByRetrospectiveId(
                retrospectiveId = retrospectiveId,
                pageInfo =
                    OffsetPageInfo(
                        page = page,
                        size = size,
                        orderBy = null,
                    ),
            )
    }

    /**
     * 특정 회원의 특정 기간과 겹치는 완료된 회고 조회
     */
    data class OffsetByMemberIdAndOverlappingPeriod(
        val memberId: MemberId,
        val periodStart: LocalDate,
        val periodEnd: LocalDate,
        override val pageInfo: OffsetPageInfo,
    ) : CompletedRetrospectivePeriodQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
    }

    /**
     * 특정 회고 ID로 조회
     */
    data class OffsetByRetrospectiveId(
        val retrospectiveId: RetrospectiveId,
        override val pageInfo: OffsetPageInfo,
    ) : CompletedRetrospectivePeriodQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
    }
}
