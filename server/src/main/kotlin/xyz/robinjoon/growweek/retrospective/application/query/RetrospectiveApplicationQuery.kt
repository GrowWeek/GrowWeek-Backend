package xyz.robinjoon.growweek.retrospective.application.query

import xyz.robinjoon.growweek.common.CursorPageInfo
import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.WeekId

sealed class RetrospectiveApplicationQuery(
    override val pageInfo: PageInfo,
) : PageQuery {
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

        fun byMemberId(
            memberId: Long,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "createdAt",
        ): CursorByMemberId =
            byMemberId(
                memberId = MemberId(memberId),
                cursor = cursor,
                size = size,
                orderBy = orderBy,
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

        fun byMemberIdAndWeekId(
            memberId: Long,
            weekId: String,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "weekId",
        ): CursorByMemberIdAndWeekId =
            byMemberIdAndWeekId(
                memberId = MemberId(memberId),
                weekId = WeekId(weekId),
                cursor = cursor,
                size = size,
                orderBy = orderBy,
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

        fun byMemberIdAndMonth(
            memberId: Long,
            year: Int,
            month: Int,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "weekId",
        ): CursorByMemberIdAndMonth =
            byMemberIdAndMonth(
                memberId = MemberId(memberId),
                year = year,
                month = month,
                cursor = cursor,
                size = size,
                orderBy = orderBy,
            )
    }

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

        fun byMemberId(
            memberId: Long,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "createdAt",
        ): OffsetByMemberId =
            byMemberId(
                memberId = MemberId(memberId),
                page = page,
                size = size,
                orderBy = orderBy,
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

        fun byMemberIdAndWeekId(
            memberId: Long,
            weekId: String,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "weekId",
        ): OffsetByMemberIdAndWeekId =
            byMemberIdAndWeekId(
                memberId = MemberId(memberId),
                weekId = WeekId(weekId),
                page = page,
                size = size,
                orderBy = orderBy,
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

        fun byMemberIdAndMonth(
            memberId: Long,
            year: Int,
            month: Int,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "weekId",
        ): OffsetByMemberIdAndMonth =
            byMemberIdAndMonth(
                memberId = MemberId(memberId),
                year = year,
                month = month,
                page = page,
                size = size,
                orderBy = orderBy,
            )
    }

    companion object {
        fun byRetrospectiveId(
            retrospectiveId: Long,
            memberId: Long,
        ): ByRetrospectiveId =
            ByRetrospectiveId(
                retrospectiveId = RetrospectiveId(retrospectiveId),
                memberId = MemberId(memberId),
            )
    }

    // 단건 조회
    data class ByRetrospectiveId(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId,
    ) : RetrospectiveApplicationQuery(OffsetPageInfo(page = 0, size = 1))

    // Cursor 기반 쿼리
    data class CursorByMemberId(
        val memberId: MemberId,
        override val pageInfo: CursorPageInfo,
    ) : RetrospectiveApplicationQuery(pageInfo)

    data class CursorByMemberIdAndWeekId(
        val memberId: MemberId,
        val weekId: WeekId,
        override val pageInfo: CursorPageInfo,
    ) : RetrospectiveApplicationQuery(pageInfo)

    data class CursorByMemberIdAndMonth(
        val memberId: MemberId,
        val year: Int,
        val month: Int,
        override val pageInfo: CursorPageInfo,
    ) : RetrospectiveApplicationQuery(pageInfo)

    // Offset 기반 쿼리
    data class OffsetByMemberId(
        val memberId: MemberId,
        override val pageInfo: OffsetPageInfo,
    ) : RetrospectiveApplicationQuery(pageInfo)

    data class OffsetByMemberIdAndWeekId(
        val memberId: MemberId,
        val weekId: WeekId,
        override val pageInfo: OffsetPageInfo,
    ) : RetrospectiveApplicationQuery(pageInfo)

    data class OffsetByMemberIdAndMonth(
        val memberId: MemberId,
        val year: Int,
        val month: Int,
        override val pageInfo: OffsetPageInfo,
    ) : RetrospectiveApplicationQuery(pageInfo)
}
