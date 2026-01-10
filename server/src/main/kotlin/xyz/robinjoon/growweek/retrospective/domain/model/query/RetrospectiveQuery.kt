package xyz.robinjoon.growweek.retrospective.domain.model.query

import xyz.robinjoon.growweek.common.CursorPageInfo
import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.WeekId

sealed class RetrospectiveQuery(
    override val pageInfo: PageInfo,
) : PageQuery {
    /**
     * Cursor 기반 쿼리 팩토리
     */
    object Cursor {
        fun byMemberId(
            memberId: MemberId,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "createdAt",
        ): CursorByMemberId =
            CursorByMemberId(
                memberId = memberId,
                pageInfo =
                    CursorPageInfo(
                        cursor = cursor,
                        size = size,
                        orderBy = orderBy,
                    ),
            )

        fun byMemberIdAndWeekId(
            memberId: MemberId,
            weekId: WeekId,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "weekId",
        ): CursorByMemberIdAndWeekId =
            CursorByMemberIdAndWeekId(
                memberId = memberId,
                weekId = weekId,
                pageInfo =
                    CursorPageInfo(
                        cursor = cursor,
                        size = size,
                        orderBy = orderBy,
                    ),
            )

        fun byRetrospectiveId(
            retrospectiveId: RetrospectiveId,
            cursor: String? = null,
            size: Int = 1,
            orderBy: String? = null,
        ): CursorByRetrospectiveId =
            CursorByRetrospectiveId(
                retrospectiveId = retrospectiveId,
                pageInfo =
                    CursorPageInfo(
                        cursor = cursor,
                        size = size,
                        orderBy = orderBy,
                    ),
            )

        fun byMemberIdAndMonth(
            memberId: MemberId,
            year: Int,
            month: Int,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "weekId",
        ): CursorByMemberIdAndMonth =
            CursorByMemberIdAndMonth(
                memberId = memberId,
                year = year,
                month = month,
                pageInfo =
                    CursorPageInfo(
                        cursor = cursor,
                        size = size,
                        orderBy = orderBy,
                    ),
            )
    }

    /**
     * Offset 기반 쿼리 팩토리
     */
    object Offset {
        fun byMemberId(
            memberId: MemberId,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "createdAt",
        ): OffsetByMemberId =
            OffsetByMemberId(
                memberId = memberId,
                pageInfo =
                    OffsetPageInfo(
                        page = page,
                        size = size,
                        orderBy = orderBy,
                    ),
            )

        fun byMemberIdAndWeekId(
            memberId: MemberId,
            weekId: WeekId,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "weekId",
        ): OffsetByMemberIdAndWeekId =
            OffsetByMemberIdAndWeekId(
                memberId = memberId,
                weekId = weekId,
                pageInfo =
                    OffsetPageInfo(
                        page = page,
                        size = size,
                        orderBy = orderBy,
                    ),
            )

        fun byRetrospectiveId(
            retrospectiveId: RetrospectiveId,
            page: Int = 0,
            size: Int = 1,
            orderBy: String? = null,
        ): OffsetByRetrospectiveId =
            OffsetByRetrospectiveId(
                retrospectiveId = retrospectiveId,
                pageInfo =
                    OffsetPageInfo(
                        page = page,
                        size = size,
                        orderBy = orderBy,
                    ),
            )

        fun byMemberIdAndMonth(
            memberId: MemberId,
            year: Int,
            month: Int,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "weekId",
        ): OffsetByMemberIdAndMonth =
            OffsetByMemberIdAndMonth(
                memberId = memberId,
                year = year,
                month = month,
                pageInfo =
                    OffsetPageInfo(
                        page = page,
                        size = size,
                        orderBy = orderBy,
                    ),
            )
    }

    // Cursor 기반 쿼리 구현체들

    data class CursorByMemberId(
        val memberId: MemberId,
        override val pageInfo: CursorPageInfo,
    ) : RetrospectiveQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByMemberIdAndWeekId(
        val memberId: MemberId,
        val weekId: WeekId,
        override val pageInfo: CursorPageInfo,
    ) : RetrospectiveQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByRetrospectiveId(
        val retrospectiveId: RetrospectiveId,
        override val pageInfo: CursorPageInfo,
    ) : RetrospectiveQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByMemberIdAndMonth(
        val memberId: MemberId,
        val year: Int,
        val month: Int,
        override val pageInfo: CursorPageInfo,
    ) : RetrospectiveQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    // Offset 기반 쿼리 구현체들

    data class OffsetByMemberId(
        val memberId: MemberId,
        override val pageInfo: OffsetPageInfo,
    ) : RetrospectiveQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByMemberIdAndWeekId(
        val memberId: MemberId,
        val weekId: WeekId,
        override val pageInfo: OffsetPageInfo,
    ) : RetrospectiveQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByRetrospectiveId(
        val retrospectiveId: RetrospectiveId,
        override val pageInfo: OffsetPageInfo,
    ) : RetrospectiveQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByMemberIdAndMonth(
        val memberId: MemberId,
        val year: Int,
        val month: Int,
        override val pageInfo: OffsetPageInfo,
    ) : RetrospectiveQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }
}
