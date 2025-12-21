package xyz.robinjoon.growweek.retrospective.application.query

import xyz.robinjoon.growweek.common.CursorPageInfo
import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.UserId
import java.time.LocalDate

sealed class RetrospectiveApplicationQuery(
    override val pageInfo: PageInfo
) : PageQuery {

    object Cursor {
        fun byUserId(
            userId: UserId,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "createdAt"
        ): CursorByUserId {
            return CursorByUserId(
                userId = userId,
                pageInfo = CursorPageInfo(
                    cursor = cursor,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byUserIdAndPeriod(
            userId: UserId,
            startDate: LocalDate,
            endDate: LocalDate,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "startDate"
        ): CursorByUserIdAndPeriod {
            return CursorByUserIdAndPeriod(
                userId = userId,
                startDate = startDate,
                endDate = endDate,
                pageInfo = CursorPageInfo(
                    cursor = cursor,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byUserIdAndMonth(
            userId: UserId,
            year: Int,
            month: Int,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "startDate"
        ): CursorByUserIdAndMonth {
            return CursorByUserIdAndMonth(
                userId = userId,
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
        fun byUserId(
            userId: UserId,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "createdAt"
        ): OffsetByUserId {
            return OffsetByUserId(
                userId = userId,
                pageInfo = OffsetPageInfo(
                    page = page,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byUserIdAndPeriod(
            userId: UserId,
            startDate: LocalDate,
            endDate: LocalDate,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "startDate"
        ): OffsetByUserIdAndPeriod {
            return OffsetByUserIdAndPeriod(
                userId = userId,
                startDate = startDate,
                endDate = endDate,
                pageInfo = OffsetPageInfo(
                    page = page,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byUserIdAndMonth(
            userId: UserId,
            year: Int,
            month: Int,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "startDate"
        ): OffsetByUserIdAndMonth {
            return OffsetByUserIdAndMonth(
                userId = userId,
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
        val userId: UserId
    ) : RetrospectiveApplicationQuery(OffsetPageInfo(page = 0, size = 1))

    // Cursor 기반 쿼리
    data class CursorByUserId(
        val userId: UserId,
        override val pageInfo: CursorPageInfo
    ) : RetrospectiveApplicationQuery(pageInfo)

    data class CursorByUserIdAndPeriod(
        val userId: UserId,
        val startDate: LocalDate,
        val endDate: LocalDate,
        override val pageInfo: CursorPageInfo
    ) : RetrospectiveApplicationQuery(pageInfo)

    data class CursorByUserIdAndMonth(
        val userId: UserId,
        val year: Int,
        val month: Int,
        override val pageInfo: CursorPageInfo
    ) : RetrospectiveApplicationQuery(pageInfo)

    // Offset 기반 쿼리
    data class OffsetByUserId(
        val userId: UserId,
        override val pageInfo: OffsetPageInfo
    ) : RetrospectiveApplicationQuery(pageInfo)

    data class OffsetByUserIdAndPeriod(
        val userId: UserId,
        val startDate: LocalDate,
        val endDate: LocalDate,
        override val pageInfo: OffsetPageInfo
    ) : RetrospectiveApplicationQuery(pageInfo)

    data class OffsetByUserIdAndMonth(
        val userId: UserId,
        val year: Int,
        val month: Int,
        override val pageInfo: OffsetPageInfo
    ) : RetrospectiveApplicationQuery(pageInfo)
}
