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
        ): CursorByMemberId = byMemberId(MemberId(memberId), cursor, size, orderBy)

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
        ): CursorByMemberIdAndWeekId = byMemberIdAndWeekId(MemberId(memberId), WeekId(weekId), cursor, size, orderBy)

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
        ): CursorByMemberIdAndMonth = byMemberIdAndMonth(MemberId(memberId), year, month, cursor, size, orderBy)
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
        ): OffsetByMemberId = byMemberId(MemberId(memberId), page, size, orderBy)

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
        ): OffsetByMemberIdAndWeekId = byMemberIdAndWeekId(MemberId(memberId), WeekId(weekId), page, size, orderBy)

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
        ): OffsetByMemberIdAndMonth = byMemberIdAndMonth(MemberId(memberId), year, month, page, size, orderBy)
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
}
