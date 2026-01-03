package xyz.robinjoon.growweek.task.domain.model.query

import xyz.robinjoon.growweek.common.CursorPageInfo
import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.TaskId
import java.time.LocalDate

sealed class TaskQuery(
    override val pageInfo: PageInfo
) : PageQuery {

    /**
     * Cursor 기반 쿼리 팩토리
     */
    object Cursor {
        fun byMemberId(
            memberId: MemberId,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "updatedAt"
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

        fun byMemberIdAndWeek(
            memberId: MemberId,
            weekStart: LocalDate,
            weekEnd: LocalDate,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "priority"
        ): CursorByMemberIdAndWeek {
            return CursorByMemberIdAndWeek(
                memberId = memberId,
                weekStart = weekStart,
                weekEnd = weekEnd,
                pageInfo = CursorPageInfo(
                    cursor = cursor,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byTaskId(
            taskId: TaskId,
            cursor: String? = null,
            size: Int = 1,
            orderBy: String? = null
        ): CursorByTaskId {
            return CursorByTaskId(
                taskId = taskId,
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
        fun byMemberId(
            memberId: MemberId,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "updatedAt"
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

        fun byMemberIdAndWeek(
            memberId: MemberId,
            weekStart: LocalDate,
            weekEnd: LocalDate,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "priority"
        ): OffsetByMemberIdAndWeek {
            return OffsetByMemberIdAndWeek(
                memberId = memberId,
                weekStart = weekStart,
                weekEnd = weekEnd,
                pageInfo = OffsetPageInfo(
                    page = page,
                    size = size,
                    orderBy = orderBy
                )
            )
        }

        fun byTaskId(
            taskId: TaskId,
            page: Int = 0,
            size: Int = 1,
            orderBy: String? = null
        ): OffsetByTaskId {
            return OffsetByTaskId(
                taskId = taskId,
                pageInfo = OffsetPageInfo(
                    page = page,
                    size = size,
                    orderBy = orderBy
                )
            )
        }
    }

    // Cursor 기반 쿼리 구현체들

    data class CursorByMemberId(
        val memberId: MemberId,
        override val pageInfo: CursorPageInfo
    ) : TaskQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByMemberIdAndWeek(
        val memberId: MemberId,
        val weekStart: LocalDate,
        val weekEnd: LocalDate,
        override val pageInfo: CursorPageInfo
    ) : TaskQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByTaskId(
        val taskId: TaskId,
        override val pageInfo: CursorPageInfo
    ) : TaskQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    // Offset 기반 쿼리 구현체들

    data class OffsetByMemberId(
        val memberId: MemberId,
        override val pageInfo: OffsetPageInfo
    ) : TaskQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByMemberIdAndWeek(
        val memberId: MemberId,
        val weekStart: LocalDate,
        val weekEnd: LocalDate,
        override val pageInfo: OffsetPageInfo
    ) : TaskQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByTaskId(
        val taskId: TaskId,
        override val pageInfo: OffsetPageInfo
    ) : TaskQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }
}
