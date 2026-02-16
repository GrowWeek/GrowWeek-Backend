package xyz.robinjoon.growweek.task.application.query

import xyz.robinjoon.growweek.common.CursorPageInfo
import xyz.robinjoon.growweek.common.OffsetPageInfo
import xyz.robinjoon.growweek.common.PageInfo
import xyz.robinjoon.growweek.common.PageQuery
import xyz.robinjoon.growweek.common.domain.MemberId
import xyz.robinjoon.growweek.common.domain.TaskId
import xyz.robinjoon.growweek.common.domain.WeekId

sealed class TaskApplicationQuery(
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
            orderBy: String? = "updatedAt",
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
            orderBy: String? = "updatedAt",
        ): CursorByMemberId = byMemberId(MemberId(memberId), cursor, size, orderBy)

        fun byMemberIdAndWeek(
            memberId: MemberId,
            weekId: WeekId,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "priority",
        ): CursorByMemberIdAndWeek =
            CursorByMemberIdAndWeek(
                memberId = memberId,
                weekId = weekId,
                pageInfo =
                    CursorPageInfo(
                        cursor = cursor,
                        size = size,
                        orderBy = orderBy,
                    ),
            )

        fun byMemberIdAndWeek(
            memberId: Long,
            weekId: String,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "priority",
        ): CursorByMemberIdAndWeek = byMemberIdAndWeek(MemberId(memberId), WeekId(weekId), cursor, size, orderBy)

        fun byTaskId(
            taskId: TaskId,
            memberId: MemberId,
            cursor: String? = null,
            size: Int = 1,
        ): CursorByTaskId =
            CursorByTaskId(
                taskId = taskId,
                memberId = memberId,
                pageInfo =
                    CursorPageInfo(
                        cursor = cursor,
                        size = size,
                        orderBy = null,
                    ),
            )

        fun byTaskId(
            taskId: Long,
            memberId: Long,
            cursor: String? = null,
            size: Int = 1,
        ): CursorByTaskId = byTaskId(TaskId(taskId), MemberId(memberId), cursor, size)
    }

    /**
     * Offset 기반 쿼리 팩토리
     */
    object Offset {
        fun byMemberId(
            memberId: MemberId,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "updatedAt",
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
            orderBy: String? = "updatedAt",
        ): OffsetByMemberId = byMemberId(MemberId(memberId), page, size, orderBy)

        fun byMemberIdAndWeek(
            memberId: MemberId,
            weekId: WeekId,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "priority",
        ): OffsetByMemberIdAndWeek =
            OffsetByMemberIdAndWeek(
                memberId = memberId,
                weekId = weekId,
                pageInfo =
                    OffsetPageInfo(
                        page = page,
                        size = size,
                        orderBy = orderBy,
                    ),
            )

        fun byMemberIdAndWeek(
            memberId: Long,
            weekId: String,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "priority",
        ): OffsetByMemberIdAndWeek = byMemberIdAndWeek(MemberId(memberId), WeekId(weekId), page, size, orderBy)

        fun byTaskId(
            taskId: TaskId,
            memberId: MemberId,
            page: Int = 0,
            size: Int = 1,
        ): OffsetByTaskId =
            OffsetByTaskId(
                taskId = taskId,
                memberId = memberId,
                pageInfo =
                    OffsetPageInfo(
                        page = page,
                        size = size,
                        orderBy = null,
                    ),
            )

        fun byTaskId(
            taskId: Long,
            memberId: Long,
            page: Int = 0,
            size: Int = 1,
        ): OffsetByTaskId = byTaskId(TaskId(taskId), MemberId(memberId), page, size)
    }

    // Cursor 기반 쿼리 구현체들

    data class CursorByMemberId(
        val memberId: MemberId,
        override val pageInfo: CursorPageInfo,
    ) : TaskApplicationQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByMemberIdAndWeek(
        val memberId: MemberId,
        val weekId: WeekId,
        override val pageInfo: CursorPageInfo,
    ) : TaskApplicationQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByTaskId(
        val taskId: TaskId,
        val memberId: MemberId,
        override val pageInfo: CursorPageInfo,
    ) : TaskApplicationQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    // Offset 기반 쿼리 구현체들

    data class OffsetByMemberId(
        val memberId: MemberId,
        override val pageInfo: OffsetPageInfo,
    ) : TaskApplicationQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByMemberIdAndWeek(
        val memberId: MemberId,
        val weekId: WeekId,
        override val pageInfo: OffsetPageInfo,
    ) : TaskApplicationQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByTaskId(
        val taskId: TaskId,
        val memberId: MemberId,
        override val pageInfo: OffsetPageInfo,
    ) : TaskApplicationQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }
}
