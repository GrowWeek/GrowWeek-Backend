package xyz.robinjoon.growweek.retrospective.domain.model.query

import xyz.robinjoon.growweek.common.CursorPageInfo
import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.RetrospectiveId
import xyz.robinjoon.growweek.common.domain.UserId
import java.time.LocalDate

sealed class RetrospectiveQuery(
    override val pageInfo: PageInfo
) : PageQuery {

    /**
     * Cursor 기반 쿼리 팩토리
     */
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

        fun byRetrospectiveId(
            retrospectiveId: RetrospectiveId,
            cursor: String? = null,
            size: Int = 1,
            orderBy: String? = null
        ): CursorByRetrospectiveId {
            return CursorByRetrospectiveId(
                retrospectiveId = retrospectiveId,
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

    /**
     * Offset 기반 쿼리 팩토리
     */
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

        fun byRetrospectiveId(
            retrospectiveId: RetrospectiveId,
            page: Int = 0,
            size: Int = 1,
            orderBy: String? = null
        ): OffsetByRetrospectiveId {
            return OffsetByRetrospectiveId(
                retrospectiveId = retrospectiveId,
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

    // Cursor 기반 쿼리 구현체들

    data class CursorByUserId(
        val userId: UserId,
        override val pageInfo: CursorPageInfo
    ) : RetrospectiveQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByUserIdAndPeriod(
        val userId: UserId,
        val startDate: LocalDate,
        val endDate: LocalDate,
        override val pageInfo: CursorPageInfo
    ) : RetrospectiveQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByRetrospectiveId(
        val retrospectiveId: RetrospectiveId,
        override val pageInfo: CursorPageInfo
    ) : RetrospectiveQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByUserIdAndMonth(
        val userId: UserId,
        val year: Int,
        val month: Int,
        override val pageInfo: CursorPageInfo
    ) : RetrospectiveQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    // Offset 기반 쿼리 구현체들

    data class OffsetByUserId(
        val userId: UserId,
        override val pageInfo: OffsetPageInfo
    ) : RetrospectiveQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByUserIdAndPeriod(
        val userId: UserId,
        val startDate: LocalDate,
        val endDate: LocalDate,
        override val pageInfo: OffsetPageInfo
    ) : RetrospectiveQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByRetrospectiveId(
        val retrospectiveId: RetrospectiveId,
        override val pageInfo: OffsetPageInfo
    ) : RetrospectiveQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByUserIdAndMonth(
        val userId: UserId,
        val year: Int,
        val month: Int,
        override val pageInfo: OffsetPageInfo
    ) : RetrospectiveQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }
}
