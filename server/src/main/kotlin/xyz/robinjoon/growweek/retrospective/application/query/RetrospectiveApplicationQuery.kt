package xyz.robinjoon.growweek.retrospective.application.query

import xyz.robinjoon.growweek.common.CursorPageInfo
import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.MemberId
import java.time.LocalDate

sealed class RetrospectiveApplicationQuery(
    override val pageInfo: PageInfo
) : PageQuery {

    object Cursor {
        fun byMemberId(
            memberId: MemberId,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "createdAt"
        ): CursorByMemberId {
            return CursorByMemberId(
                memberId = memberId,
                pageInfo = CursorPageInfo(
                    cursor = cursor,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byMemberIdAndPeriod(
            memberId: MemberId,
            startDate: LocalDate,
            endDate: LocalDate,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "startDate"
        ): CursorByMemberIdAndPeriod {
            return CursorByMemberIdAndPeriod(
                memberId = memberId,
                startDate = startDate,
                endDate = endDate,
                pageInfo = CursorPageInfo(
                    cursor = cursor,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byMemberIdAndMonth(
            memberId: MemberId,
            year: Int,
            month: Int,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "startDate"
        ): CursorByMemberIdAndMonth {
            return CursorByMemberIdAndMonth(
                memberId = memberId,
                year = year,
                month = month,
                pageInfo = CursorPageInfo(
                    cursor = cursor,
                    size = size,
                    orderBy = orderBy
                )
            )
        }
    }

    object Offset {
        fun byMemberId(
            memberId: MemberId,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "createdAt"
        ): OffsetByMemberId {
            return OffsetByMemberId(
                memberId = memberId,
                pageInfo = OffsetPageInfo(
                    page = page,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byMemberIdAndPeriod(
            memberId: MemberId,
            startDate: LocalDate,
            endDate: LocalDate,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "startDate"
        ): OffsetByMemberIdAndPeriod {
            return OffsetByMemberIdAndPeriod(
                memberId = memberId,
                startDate = startDate,
                endDate = endDate,
                pageInfo = OffsetPageInfo(
                    page = page,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byMemberIdAndMonth(
            memberId: MemberId,
            year: Int,
            month: Int,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "startDate"
        ): OffsetByMemberIdAndMonth {
            return OffsetByMemberIdAndMonth(
                memberId = memberId,
                year = year,
                month = month,
                pageInfo = OffsetPageInfo(
                    page = page,
                    size = size,
                    orderBy = orderBy
                )
            )
        }
    }

    // 단건 조회
    data class ByRetrospectiveId(
        val retrospectiveId: RetrospectiveId,
        val memberId: MemberId
    ) : RetrospectiveApplicationQuery(OffsetPageInfo(page = 0, size = 1))

    // Cursor 기반 쿼리
    data class CursorByMemberId(
        val memberId: MemberId,
        override val pageInfo: CursorPageInfo
    ) : RetrospectiveApplicationQuery(pageInfo)

    data class CursorByMemberIdAndPeriod(
        val memberId: MemberId,
        val startDate: LocalDate,
        val endDate: LocalDate,
        override val pageInfo: CursorPageInfo
    ) : RetrospectiveApplicationQuery(pageInfo)

    data class CursorByMemberIdAndMonth(
        val memberId: MemberId,
        val year: Int,
        val month: Int,
        override val pageInfo: CursorPageInfo
    ) : RetrospectiveApplicationQuery(pageInfo)

    // Offset 기반 쿼리
    data class OffsetByMemberId(
        val memberId: MemberId,
        override val pageInfo: OffsetPageInfo
    ) : RetrospectiveApplicationQuery(pageInfo)

    data class OffsetByMemberIdAndPeriod(
        val memberId: MemberId,
        val startDate: LocalDate,
        val endDate: LocalDate,
        override val pageInfo: OffsetPageInfo
    ) : RetrospectiveApplicationQuery(pageInfo)

    data class OffsetByMemberIdAndMonth(
        val memberId: MemberId,
        val year: Int,
        val month: Int,
        override val pageInfo: OffsetPageInfo
    ) : RetrospectiveApplicationQuery(pageInfo)
}
