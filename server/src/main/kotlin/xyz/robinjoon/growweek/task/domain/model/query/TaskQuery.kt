package xyz.robinjoon.growweek.task.domain.model.query

import xyz.robinjoon.growweek.common.*
import xyz.robinjoon.growweek.common.domain.UserId
import xyz.robinjoon.growweek.common.domain.TaskId
import java.time.LocalDate

sealed class TaskQuery(
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
            orderBy: String? = "updatedAt"
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

        fun byUserIdAndWeek(
            userId: UserId,
            weekStart: LocalDate,
            weekEnd: LocalDate,
            cursor: String? = null,
            size: Int = 20,
            orderBy: String? = "priority"
        ): CursorByUserIdAndWeek {
            return CursorByUserIdAndWeek(
                userId = userId,
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
        fun byUserId(
            userId: UserId,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "updatedAt"
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

        fun byUserIdAndWeek(
            userId: UserId,
            weekStart: LocalDate,
            weekEnd: LocalDate,
            page: Int = 0,
            size: Int = 20,
            orderBy: String? = "priority"
        ): OffsetByUserIdAndWeek {
            return OffsetByUserIdAndWeek(
                userId = userId,
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

    data class CursorByUserId(
        val userId: UserId,
        override val pageInfo: CursorPageInfo
    ) : TaskQuery(pageInfo) {
        val cursor get() = pageInfo.cursor
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class CursorByUserIdAndWeek(
        val userId: UserId,
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

    data class OffsetByUserId(
        val userId: UserId,
        override val pageInfo: OffsetPageInfo
    ) : TaskQuery(pageInfo) {
        val page get() = pageInfo.page
        val size get() = pageInfo.size
        val orderBy: String? get() = pageInfo.orderBy
    }

    data class OffsetByUserIdAndWeek(
        val userId: UserId,
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
