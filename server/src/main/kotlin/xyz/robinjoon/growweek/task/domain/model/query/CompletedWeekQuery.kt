package xyz.robinjoon.growweek.task.domain.model.query

import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.WeekId

/**
 * 완료된 회고 주 조회 Query
 */
sealed class CompletedWeekQuery(
    override val pageInfo: PageInfo,
) : PageQuery {
    /**
     * Offset 기반 쿼리 팩토리
     */
    object Offset {
        /**
         * 특정 회원의 특정 주에 완료된 회고 조회
         */
        fun byMemberIdAndWeekId(
            memberId: MemberId,
            weekId: WeekId,
            page: Int = 0,
            size: Int = 100,
        ): OffsetByMemberIdAndWeekId =
            OffsetByMemberIdAndWeekId(
                memberId = memberId,
                weekId = weekId,
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
     * 특정 회원의 특정 주에 완료된 회고 조회
     */
    data class OffsetByMemberIdAndWeekId(
        val memberId: MemberId,
        val weekId: WeekId,
        override val pageInfo: OffsetPageInfo,
    ) : CompletedWeekQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
    }

    /**
     * 특정 회고 ID로 조회
     */
    data class OffsetByRetrospectiveId(
        val retrospectiveId: RetrospectiveId,
        override val pageInfo: OffsetPageInfo,
    ) : CompletedWeekQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
    }
}
