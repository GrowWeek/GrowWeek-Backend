package xyz.robinjoon.growweek.task.application.query

import xyz.robinjoon.growweek.common.CursorPageInfo
import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.TaskId
import java.time.LocalDate

sealed class TaskApplicationQuery(
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
            memberId: MemberId,
            cursor: String? = null,
            size: Int = 1
        ): CursorByTaskId {
            return CursorByTaskId(
                taskId = taskId,
                memberId = memberId,
                pageInfo = CursorPageInfo(
                    cursor = cursor,
                    size = size,
                    orderBy = null
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
            memberId: MemberId,
            page: Int = 0,
            size: Int = 1
        ): OffsetByTaskId {
            return OffsetByTaskId(
                taskId = taskId,
                memberId = memberId,
                pageInfo = OffsetPageInfo(
                    page = page,
                    size = size,
                    orderBy = null
                )
            )
        }
    }

    // Cursor 기반 쿼리 구현체들

    data class CursorByMemberId(
        val memberId: MemberId,
        override val pageInfo: CursorPageInfo
    ) : TaskApplicationQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByMemberIdAndWeek(
        val memberId: MemberId,
        val weekStart: LocalDate,
        val weekEnd: LocalDate,
        override val pageInfo: CursorPageInfo
    ) : TaskApplicationQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByTaskId(
        val taskId: TaskId,
        val memberId: MemberId,
        override val pageInfo: CursorPageInfo
    ) : TaskApplicationQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    // Offset 기반 쿼리 구현체들

    data class OffsetByMemberId(
        val memberId: MemberId,
        override val pageInfo: OffsetPageInfo
    ) : TaskApplicationQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByMemberIdAndWeek(
        val memberId: MemberId,
        val weekStart: LocalDate,
        val weekEnd: LocalDate,
        override val pageInfo: OffsetPageInfo
    ) : TaskApplicationQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByTaskId(
        val taskId: TaskId,
        val memberId: MemberId,
        override val pageInfo: OffsetPageInfo
    ) : TaskApplicationQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }
}
